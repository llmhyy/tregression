#!/bin/bash

# Description:
# Prints all project names and bug numbers in regs4j

# Usage:
# 1. Run the command: <this script> <path to CLI.sh>
# e.g. ./regs4j.sh ./CLI.sh

# ====================== Configuration =====================
reverse=0
# ==========================================================

cliDir="$(dirname $1)"
cd $cliDir
echo "entered $cliDir directory"

cliCommand="./$(basename $1)"
echo running $cliCommand
echo ' '

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
    projName=${project/\//_}
	echo -n "\"$projName\", "
done
echo ' '
echo ' '
echo Printing bug numbers...
echo ' '
for project in ${projects[@]}
do
	numOfRegs=$(echo "use $project" | "$cliCommand")
	numOfRegs=${numOfRegs/*"regressions... "}
	numOfRegs=${numOfRegs/' '*}
    echo -n "$numOfRegs, "
done
