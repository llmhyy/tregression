#!/bin/bash

# Usage:
# 1. Update the configuration below to match your system's.
# 2. Run the command: <this script> <path to CLI.sh>
# e.g. ./regs4j.sh ./CLI.sh

# ====================== Configuration =====================
# Change the paths below to your system's
repoDirToPasteTo='/media/sf_VBox-Shared/reg4j'
reverse=0
# ==========================================================

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
echo Starting cloning...
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
		strRepRFC=$project/$j/rfc
		strRepRIC=$project/$j/ric
		csvRowRFC=$strRepRFC
		csvRowRIC=$strRepRIC
		echo checking out $j for $project
		paths=$({ echo "use $project"; echo "checkout $j"; } | "$cliCommand")
		pathToRFC=${paths/*"rfc directory:"}
		pathToRFC=${pathToRFC/rfc*/rfc}
		pathToRIC=${paths/*"ric directory:"}
		pathToRIC=${pathToRIC/ric*/ric}
		echo path to rfc: $pathToRFC
		echo path to ric: $pathToRIC

		projectDir=${project/\//-}
		newPath=$repoDirToPasteTo/$projectDir/$j
		mkdir -p $newPath

		echo copying rfc to $newPath/rfc
		cp -r $pathToRFC $newPath

		echo copying ric to $newPath/ric
		cp -r $pathToRIC $newPath

        echo "${tests[$((j-1))]}" > $newPath/$testFileName
	done
done
