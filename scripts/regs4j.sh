#!/bin/bash

# Description:
# Clones working and RIC commits from each project in regs4j to your specified directory, then runs maven test -Dtest=ClassName#testName on each of them
# Compilation failures, checkout failures, and other error messages are recorded on your specified CSV file
# It will check the CSV file if a bug in a project already passed. Otherwise, it will clone and compile it again, and replace the corresponding row with the new result if it was in the CSV.

# Usage:
# 1. Update the configuration below to match your system's.
# 2. Run the command: <this script> <path to regs4j's CLI.sh>. If not specified, "./CLI.sh" is used.
# e.g. ./regs4j.sh ./CLI.sh

# ====================== Configuration =====================
# Change the paths below to your system's
repoDirToPasteTo="/mnt/c/Users/Chenghin/Desktop/VBox-Shared/regs4j"
reverse=0
csvFile="/mnt/c/Users/Chenghin/Desktop/VBox-Shared/regs4j.csv"
rerunFailing=0
# ==========================================================

firstLineInCSV="Project,Type,BugId,Regs4jError,Reason,Logs"
if ! [[ -f $csvFile ]]
then
    echo 'CSV does not exist, creating a new one'
    echo $firstLineInCSV > $csvFile
else
    prevCSVContents=$(cat $csvFile)
fi

# If path to regs4j shell script not provided, use the default
if [ -z $1 ]
then
    set -- "./CLI.sh"
fi

cliDir="$(dirname $1)"
cd $cliDir
echo "entered $cliDir directory"

cliCommand="./$(basename $1)"
echo running $cliCommand
echo ' '

testFileName="tests.txt"

projects=$(echo projects | "$cliCommand")
projects=${projects/*Done}
projects=${projects/RegMiner*}
projects=( $(echo $projects))
if [ $reverse -eq 1 ]
then
	min=0
	max=$((${#projects[@]} - 1))
	while [[ min -lt max ]]
	do
		x="${projects[$min]}"
		projects[$min]="${projects[$max]}"
		projects[$max]="$x"
		(( min++, max--))
	done
fi
echo "Printing projects..."
echo ' '
for project in ${projects[@]}
do
	echo $project
done
echo ' '
echo Start cloning...
echo ' '
for project in ${projects[@]}
do
    echo using project $project
    numOfRegs=$(echo "use $project" | "$cliCommand")
    numOfRegs=${numOfRegs/*"regressions... "}
    numOfRegs=${numOfRegs/' '*}
    echo num of regressions: $numOfRegs
    listOutput=$({ echo "use $project"; echo "list"; } | "$cliCommand")
    tests=()
    testCaseStr=" testcase: "
    for fragment in $listOutput; do
        if [[ "$fragment" =~ .*"#".* ]]; then
            tests+=($fragment)
        fi
    done
	for j in $(seq 1 $numOfRegs)
	do
		strRepWork=$project/$j/work
		strRepRIC=$project/$j/ric
		csvRowWorkPrefix=$project,work,$j,
		csvRowRICPrefix=$project,ric,$j,
		csvRowWork=$csvRowWorkPrefix
		csvRowRIC=$csvRowRICPrefix
        if [ $rerunFailing -eq 0 ]
        then
            if [[ $prevCSVContents == *"$csvRowWork"* && $prevCSVContents == *"$csvRowRIC"* ]]
            then
                echo $project with bug id $j already in csv file, skipping...
                continue
            fi
        fi

		if [[ $prevCSVContents == *"$csvRowWork"FALSE* && $prevCSVContents == *"$csvRowRIC"FALSE* ]]
		then
		    echo $project with bug id $j already passing in csv file, skipping...
		    continue
		fi
        workAlreadyInCSV=0
        ricAlreadyInCSV=0
		if [[ $prevCSVContents == *"$csvRowWork"* ]]
		then
            workAlreadyInCSV=1
		fi
		if [[ $prevCSVContents == *"$csvRowRIC"* ]]
		then
            ricAlreadyInCSV=1
		fi
        echo checking out $j for $project
        checkoutResult=$({ echo "use $project"; echo "checkout $j"; } | "$cliCommand")
		if [[ $checkoutResult == *"Please specify a project before checking out a bug"* ]]
		then
		    repoNotFoundMsg="Repository not found"
		    echo $repoNotFoundMsg 
		    csvRowWork+=TRUE,$repoNotFoundMsg,
		    csvRowRIC+=TRUE,$repoNotFoundMsg,
		    echo $csvRowWork >> $csvFile
		    echo $csvRowRIC >> $csvFile
		    continue
		fi
		pathToWorking=${checkoutResult/*"work directory:"}
		pathToWorking=${pathToWorking/work*/work}
		pathToRIC=${checkoutResult/*"ric directory:"}
		pathToRIC=${pathToRIC/ric*/ric}
		echo path to work: $pathToWorking
		echo path to ric: $pathToRIC

		projectDir=${project/\//_}

		newPath=$repoDirToPasteTo/$projectDir/$j
		mkdir -p $newPath

		echo copying working to $newPath/work
		cp -r $pathToWorking $newPath

		echo copying ric to $newPath/ric
		cp -r $pathToRIC $newPath

		testCase="${tests[$((j-1))]}"
		testCaseRunningStr='Tests run'
		testPassStr='Failures: 0, Errors: 0'
		commitStr="working commit"
        timeoutDuration=1000
		echo "compiling $commitStr"
		mvnOutput=$( timeout $timeoutDuration mvn test -Dtest=$testCase --file $newPath/work/pom.xml | tee /dev/fd/2)
		mvnOutput=$( echo "$mvnOutput" | tr '\n' '^'  | tr -d '\r' ) # Replace new lines with another char, since it creates another row in csv
		mvnOutput=${mvnOutput//\"/\"\"} # Replace all double quotes, with double double quotes, for CSV
		if [[ $mvnOutput == *"$testCaseRunningStr"* ]]
		then
		    if [[ $mvnOutput == *"$testPassStr"* ]]
		    then
                echo "Test case passed as expected for $commitStr"	
                csvRowWork+=FALSE,,\"$mvnOutput\" # Add quotes so that inner commas are not used to create columns in csv
		    else
                echo "Test case unexpectedly failed for $commitStr"
                csvRowWork+=TRUE,"Test case failed for working commit",\"$mvnOutput\"
		    fi
		else
		    echo "mvn build failure for $commitStr"
		    csvRowWork+=TRUE,"Build failure",\"$mvnOutput\"
		fi
        if [ $workAlreadyInCSV -eq 1 ]
        then
            sed -i "s|.*$csvRowWorkPrefix.*|$csvRowWork|" $csvFile
        else
            echo $csvRowWork >> $csvFile
        fi
		commitStr="ric commit"
		echo compiling $commitStr
        isTimeout=0
		mvnOutput=$( timeout $timeoutDuration mvn test -Dtest=$testCase --file $newPath/ric/pom.xml | tee /dev/fd/2 || isTimeout=1 )
		mvnOutput=$( echo "$mvnOutput" | tr '\n' '^' | tr -d '\r') 
		mvnOutput=${mvnOutput//\"/\"\"} # Replace all double quotes, with double double quotes, for CSV
		if [[ $mvnOutput == *"$testCaseRunningStr"* ]]
		then
		    if [[ $mvnOutput != *"$testPassStr"* ]]
		    then
                echo "Test case failed as expected for $commitStr"	
                csvRowRIC+=FALSE,,\"$mvnOutput\" # Add quotes so that inner commas are not used to create columns in csv
		    else
                echo "Test case unexpectedly passed for $commitStr"
                csvRowRIC+=TRUE,"Test case passed for RIC commit",\"$mvnOutput\"
		    fi
        elif [[ $isTimeout -eq 1 ]]
        then
            echo "Test case failed as expected for $commitStr"	
            csvRowRIC+=FALSE,,\"$mvnOutput\"
		else
		    echo "mvn build failure for $commitStr"
		    csvRowRIC+=TRUE,"Build failure",\"$mvnOutput\"
		fi
        if [ $ricAlreadyInCSV -eq 1 ]
        then
            sed -i "s|.*$csvRowRICPrefix.*|$csvRowRIC|" $csvFile
        else
            echo $csvRowRIC >> $csvFile
        fi

		echo "${tests[$((j-1))]}" > $newPath/$testFileName
	done
done
