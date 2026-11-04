# install fastlane via bundle
# cd fastlane
# bundle config set path fastlane
# bundle install 

# run fastlane
# bundle exec fastlane 

# update fastlane
# bundle update fastlane

# update plugins
# bundle exec fastlane update_plugins

BUILD_NUMBER_RC_START = 51
BUILD_NUMBER_FINAL_START = 90

MAJOR_MULTIPLIER = 10000000
MINOR_MULTIPLIER = 10000
PATCH_MULTIPLIER = 100

TYPE_ALPHA = "alpha"
TYPE_RC = "rc"
TYPE_FINAL = "final"

BUNDLE_PATH = "app/build/outputs/bundle/gplayRelease/app-gplay-release.aab"

default_platform(:android)

ENV['SUPPLY_UPLOAD_MAX_RETRIES']='10'

platform :android do
    desc "Run tests and build app"
    lane :releasePhase1 do
    	sh("rm -rf ../release/ && mkdir -p ../release")
		gradle(task: 'clean')
        
        versionInfo = getVersionInfo()
        test() # needs to be overwritten in apps
        
        buildBundle()
        buildAPKs(code: versionInfo["versionCode"])
    end

    desc "Print version info"
    lane :printVersionInfo do
        versionInfo = getVersionInfo()
        versionComponents = parseVersionCode(versionInfo)
        print "Version code: #{versionInfo["versionCode"]}\n"
        print "Version name: #{versionInfo["versionName"]}\n"
        print "Major: #{versionComponents["major"]}\n"
        print "Minor: #{versionComponents["minor"]}\n"
        print "Patch: #{versionComponents["patch"]}\n"
        print "Build: #{versionComponents["build"]}\n"
    end

    # Usage: fastlane incrementVersion type:(major|minor|patch|rc|final)
    # For major, minor, and patch: will increment that version number by 1 and set the smaller ones to 0
    # For rc, final: will set build number to first rc/first final or increment it by 1
    desc "Increment version code and version name"
    lane :incrementVersion do |options|
        versionInfo = getVersionInfo()
        versionComponents = parseVersionCode(versionInfo)
        newVersionComponents = incrementVersionComponents(versionComponents: versionComponents, type: options[:type])
        versionNameGenerated = generateVersionName(newVersionComponents)
        versionCodeGenerated = generateVersionCode(newVersionComponents)

        promptYesNo(text: "Version code: #{versionInfo["versionCode"]} -> #{versionCodeGenerated}\n" +
                        "Version name: #{versionInfo["versionName"]} -> #{versionNameGenerated}"
        )
        writeVersions(versionCode: versionCodeGenerated, versionName: versionNameGenerated)
    end
    
    desc "Upload Alpha version to play store"
	lane :uploadAlphaToPlayStore do |options|
		upload_to_play_store(
			skip_upload_images: true,
			skip_upload_aab: true,
			skip_upload_changelogs: true,
			skip_upload_metadata: true,
			skip_upload_screenshots: true,
			track: 'alpha',
			apk: "/home/androiddaily/apks-#{APP}/android-#{APP}-" + options[:version] + ".apk",
		)
	end


    desc "Parse major, minor, patch and build from versionCode"
    private_lane :parseVersionCode do |versionInfo|
        versionCode = versionInfo["versionCode"]
        build = versionCode % 100
        patch = (versionCode / PATCH_MULTIPLIER) % 100
        minor = (versionCode / MINOR_MULTIPLIER) % 100
        major = (versionCode / MAJOR_MULTIPLIER) % 100

        type = getVersionType(build: build)


        { "major" => major, "minor" => minor, "patch" => patch, "build" => build, "type" => type }
    end

    desc "Get version type from build number"
    private_lane :getVersionType do |options|
        build = options[:build]
        if build < BUILD_NUMBER_RC_START
            type = TYPE_ALPHA
        elsif build < BUILD_NUMBER_FINAL_START
            type = TYPE_RC
        else
            type = TYPE_FINAL
        end
        type
    end



    desc "Generate versionCode from version components"
    private_lane :generateVersionCode do |versionComponents|
        puts "Generating version code from #{versionComponents}"
        major = versionComponents["major"]
        minor = versionComponents["minor"]
        patch = versionComponents["patch"]
        build = versionComponents["build"]
        test = major * MAJOR_MULTIPLIER + minor * MINOR_MULTIPLIER + patch * PATCH_MULTIPLIER + build
        test
    end

    desc "Compute version name from version code"
    private_lane :generateVersionName do |versionComponents|
        suffix = ""
        buildNumber = versionComponents["build"]
        puts "Generating version name from #{versionComponents}\n"
        case
          when versionComponents["type"] == TYPE_RC
            rcNumber = (buildNumber - BUILD_NUMBER_RC_START) + 1
            suffix = " RC#{rcNumber}"
          when versionComponents["type"] == TYPE_ALPHA
            suffix = " Alpha#{buildNumber + 1}"
        end
        "#{versionComponents["major"]}.#{versionComponents["minor"]}.#{versionComponents["patch"]}#{suffix}"
    end

    desc "Read versions from gradle file"
    private_lane :getVersionInfo do
        File.open("../app/build.gradle.kts","r") do |file|
            text = file.read
            versionName = text.match(/versionName = "(.*)"$/)[1]
            versionCode = text.match(/versionCode = ([0-9]*)$/)[1].to_i

            { "versionCode" => versionCode, "versionName" => versionName }
        end
    end

    desc "Write versions to gradle file"
    private_lane :writeVersions do |options|
        File.open("../app/build.gradle","r+") do |file|
            text = file.read
            text.gsub!(/versionName "(.*)"$/, "versionName \"#{options[:versionName]}\"")
            text.gsub!(/versionCode ([0-9]*)$/, "versionCode #{options[:versionCode]}")
            file.rewind
            file.write(text)
            file.truncate(file.pos)
        end
    end

    private_lane :incrementVersionComponents do |options|
        versionComponents = options[:versionComponents]
        case options[:type]
        when "major"
            versionComponents["major"] = versionComponents["major"] + 1
            versionComponents["minor"] = 0
            versionComponents["patch"] = 0
            versionComponents["build"] = 0
        when "minor"
            versionComponents["minor"] = versionComponents["minor"] + 1
            versionComponents["patch"] = 0
            versionComponents["build"] = 0
        when "patch"
            versionComponents["patch"] = versionComponents["patch"] + 1
            versionComponents["build"] = 0
        when "rc"
            if versionComponents["type"] != TYPE_RC
                versionComponents["build"] = BUILD_NUMBER_RC_START
            else
                versionComponents["build"] = versionComponents["build"] + 1
            end
        when "final"
            if versionComponents["type"] != TYPE_FINAL
                versionComponents["build"] = BUILD_NUMBER_FINAL_START
            else
                versionComponents["build"] = versionComponents["build"] + 1
            end
        else
            UI.user_error!("Unknown or missing version increment type #{options[:type]}. Usage: incrementVersion type:(major|minor|patch|rc|final)")
        end
        versionComponents["type"] = getVersionType(build: versionComponents["build"])
        versionComponents
    end

    desc "Get tag name from version components"
    private_lane :getTagName do |versionComponents|
        if versionComponents["type"] == TYPE_FINAL
            tag = "#{versionComponents["major"]}.#{versionComponents["minor"]}.#{versionComponents["patch"]}"
        elsif versionComponents["type"] == TYPE_RC
            rcNumber = (versionComponents["build"] - BUILD_NUMBER_RC_START) + 1
            rcNumberPadded = "%02d" % rcNumber
            tag = "rc-#{versionComponents["major"]}.#{versionComponents["minor"]}.#{versionComponents["patch"]}-#{rcNumberPadded}"
        else
            UI.user_error!("Build number cannot be tagged: #{versionComponents["build"]}")
        end
    end

    desc "Check if version is releasable"
    private_lane :checkReleasable do |versionComponents|
        if versionComponents["type"] != TYPE_FINAL && versionComponents["type"] != TYPE_RC
            UI.user_error!("Version is not releasable: #{versionComponents["type"]}")
        end
    end

    desc "Get play store track from version type"
    private_lane :getPlayStoreTrack do |versionComponents|
        case versionComponents["type"]
        when TYPE_RC
            track = "beta"
        when TYPE_FINAL
            track = "production"
        else
            UI.user_error!("Version is not releasable: #{versionComponents["type"]}")
        end
    end
    
    desc "Run tests"
	private_lane :test do
		 gradle(task: "clean testGplayDebugUnitTest testGenericDebugUnitTest testQaDebugUnitTest")
	end
	
	desc "Build app bundle"
	private_lane :buildBundle do
		gradle(
			task: 'bundle',
			flavor: 'Gplay',
			build_type: 'Release',
			print_command: false,
			properties: GRADLE_SIGNING_PROPERTIES
			)
	end
	
	desc "Build signed APK"
	private_lane :SignedRelease do |options|
		gradle(
			task: 'assemble',
			flavor: options[:flavor],
			build_type: 'Release',
			print_command: false,
			properties: GRADLE_SIGNING_PROPERTIES
			)
			
		sh("mv ../app/build/outputs/apk/#{options[:flavor]}/release/*.apk ../release/nextcloud-#{APP}-#{options[:flavor]}-#{options[:versionCode]}.apk")
	end
	
	desc "Build all APKs"
	private_lane :buildAPKs do |options|
		SignedRelease(flavor: "generic", versionCode: options[:code])
		SignedRelease(flavor: "gplay", versionCode: options[:code])
		
		if APP == "files"
			SignedRelease(flavor: "huawei", versionCode: options[:code])
		end
	end
	
	desc "Create release tag"
        private_lane :tag do |options|
            tagName = options[:name]
            add_git_tag(
              tag: tagName,
              sign: true
            )
            push_git_tags(tag: tagName)
        end

	 desc "Upload release artifacts to Google Play"
		private_lane :uploadToPlayStore do |options|
			upload_to_play_store(
				skip_upload_images: true,
				skip_upload_apk: true,
				track: options[:track],
				aab: BUNDLE_PATH
			)
		end
		
	 desc "Check if release artifacts exist"
		private_lane :checkArtifactsExist do
			if !File.exist?("../#{BUNDLE_PATH}")
				UI.user_error!("Bundle not found at #{BUNDLE_PATH}")
			end
		end

end

private_lane :promptYesNo do |options|
    puts "\n" + options[:text]
    answer = prompt(text: "is this okay?", boolean: true)
    if !answer
        UI.user_error!("Aborting")
    end
end
