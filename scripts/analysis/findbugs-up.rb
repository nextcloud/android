## Script from https://github.com/tir38/android-lint-entropy-reducer at 07.05.2017
# adapts to drone, use git username / token as parameter

Encoding.default_external = Encoding::UTF_8
Encoding.default_internal = Encoding::UTF_8

puts "=================== starting Android FindBugs Entropy Reducer ===================="

# get args
git_user, git_token, git_branch = ARGV

# ========================  SETUP ============================

# User name for git commits made by this script.
TRAVIS_GIT_USERNAME = String.new("Drone CI server")

# File name and relative path of generated FindBugs report. Must match build.gradle file:
#   lintOptions {
#       htmlOutput file("[FILE_NAME].html")
#   }
FINDBUGS_REPORT_FILE = String.new("build/reports/spotbugs/spotbugs.html")

# File name and relative path of previous results of this script.
PREVIOUS_FINDBUGS_RESULTS_FILE=String.new("scripts/analysis/findbugs-results.txt")

# Flag to evaluate warnings. true = check warnings; false = ignore warnings
CHECK_WARNINGS = true

# File name and relative path to custom FindBugs rules; Can be null or "".
CUSTOM_FINDBUGS_FILE = String.new("")

# ================ SETUP DONE; DON'T TOUCH ANYTHING BELOW  ================

require 'fileutils'
require 'pathname'
require 'open3'

# since we need the xml-simple gem, and we want this script self-contained, let's grab it just when we need it
begin
    gem "xml-simple"
    rescue LoadError
    system("gem install xml-simple")
    Gem.clear_paths
end

require 'xmlsimple'

# run FindBugs
puts "running FindBugs..."
system './gradlew assembleGplayDebug 1>/dev/null'

# confirm that assemble ran w/out error
result = $?.to_i
if result != 0
    puts "FAIL: failed to run ./gradlew assembleGplayDebug"
    exit 1
end

system './gradlew spotbugsGplayDebugReport 1>/dev/null 2>&1'

# find FindBugs report file
findbugs_reports = Dir.glob(FINDBUGS_REPORT_FILE)
if findbugs_reports.length == 0
    puts "Findbugs HTML report not found."
    exit 1
end
findbugs_report = String.new(findbugs_reports[0])

# find number of warnings
current_warning_count = `grep -A 3 "<b>Total</b>" build/reports/spotbugs/spotbugs.html | tail -n1 | cut -f2 -d">" | cut -f1 -d"<"`.to_i
puts "found warnings: " + current_warning_count.to_s

# get warning counts from last successful build

previous_results = false

previous_findbugs_reports = Dir.glob(PREVIOUS_FINDBUGS_RESULTS_FILE)
if previous_findbugs_reports.nil? || previous_findbugs_reports.length == 0
    previous_findbugs_report = File.new(PREVIOUS_FINDBUGS_RESULTS_FILE, "w") # create for writing to later
else
    previous_findbugs_report = String.new(previous_findbugs_reports[0])

    previous_warning_count = File.open(previous_findbugs_report, &:readline).match(/[0-9]*/)[0].to_i

    if previous_warning_count.nil?
        previous_results = false
    else
        previous_results = true

        puts "previous warnings: " + previous_warning_count.to_s
    end
end

# compare previous warning count with current warning count
if previous_results == true && current_warning_count > previous_warning_count
    puts "FAIL: warning count increased"
    exit 1
end

# check if warning and error count stayed the same
if  previous_results == true && current_warning_count == previous_warning_count
    puts "SUCCESS: count stayed the same"
    exit 2
end

# warning count DECREASED
puts "SUCCESS: count decreased from " + previous_warning_count.to_s + " to " + current_warning_count.to_s

# write new results to file (will overwrite existing, or create new)
File.write(previous_findbugs_report, current_warning_count)

# push changes to github (if this script is run locally, we don't want to overwrite git username and email, so save temporarily)
previous_git_username, _ = Open3.capture2('git config user.name')
previous_git_username = previous_git_username.strip

previous_git_email, _ = Open3.capture3('git config user.email')
previous_git_email = previous_git_email.strip

# update git user name and email for this script
system ("git config --local user.name '"  + git_user + "'")
system ("git config --local user.email 'android@nextcloud.com'")
system ("git remote rm origin")
system ("git remote add origin https://" + git_user + ":" + git_token + "@github.com/nextcloud/android")

# add previous FindBugs result file to git
system ('git add ' + PREVIOUS_FINDBUGS_RESULTS_FILE)

# commit changes; Add "skip ci" so that we don't accidentally trigger another Drone build
system ('git commit -sm "Drone: update FindBugs results to reflect reduced error/warning count [skip ci]" ')

# push to origin
system ('git push origin HEAD:' + git_branch)

# restore previous git user name and email
system("git config --local user.name '#{previous_git_username}'")
system("git config --local user.email '#{previous_git_email}'")

puts "SUCCESS: count was reduced"
exit 0 # success
