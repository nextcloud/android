## Script originally from https://github.com/tir38/android-lint-entropy-reducer at 07.05.2017
# heavily modified since then

Encoding.default_external = Encoding::UTF_8
Encoding.default_internal = Encoding::UTF_8

puts "=================== starting Android Spotbugs Entropy Reducer ===================="

# get args
git_branch = ARGV[0]

# ========================  SETUP ============================

# File name and relative path of generated Spotbugs report. Must match build.gradle file:
#   lintOptions {
#       htmlOutput file("[FILE_NAME].html")
#   }
SPOTBUGS_REPORT_FILE = String.new("app/build/reports/spotbugs/spotbugs.html")

# ================ SETUP DONE; DON'T TOUCH ANYTHING BELOW  ================

require 'fileutils'
require 'pathname'
require 'open3'

# run Spotbugs
puts "running Spotbugs..."
system './gradlew spotbugsGplayDebug 1>/dev/null 2>&1'

# find Spotbugs report file
spotbugs_reports = Dir.glob(SPOTBUGS_REPORT_FILE)
if spotbugs_reports.length == 0
    puts "Spotbugs HTML report not found."
    exit 1
end
spotbugs_report = String.new(spotbugs_reports[0])

# find number of warnings
current_warning_count = `./scripts/analysis/spotbugsSummary.py --total`.to_i
puts "found warnings: " + current_warning_count.to_s

# get warning counts from target branch
previous_xml = "/tmp/#{git_branch}.xml"
previous_results = File.file?(previous_xml)

if previous_results == true
    previous_warning_count = `./scripts/analysis/spotbugsSummary.py --total --file #{previous_xml}`.to_i
    puts "previous warnings: " + previous_warning_count.to_s
end

# compare previous warning count with current warning count
if previous_results == true && current_warning_count > previous_warning_count
    puts "FAIL: warning count increased"
    exit 1
end

# check if warning and error count stayed the same
if  previous_results == true && current_warning_count == previous_warning_count
    puts "SUCCESS: count stayed the same"
    exit 0
end

# warning count DECREASED
if previous_results == true && current_warning_count < previous_warning_count
    puts "SUCCESS: count decreased from " + previous_warning_count.to_s + " to " + current_warning_count.to_s
end
