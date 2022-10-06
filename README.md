# TRregression (ERASE)
TRregression (Trace-Based Regression ) aims to analyze execution trace to find regression bug.

![Snapshot of TRregression](/tregression/icons/screenshot.png?raw=true "Snapshot of TRregression")

A demo video is here.

[![IMAGE ALT TEXT HERE](https://img.youtube.com/vi/Uu8z3ONwRqs/0.jpg)](https://www.youtube.com/watch?v=Uu8z3ONwRqs)

Audience can also check https://youtu.be/Mte08XIETlU to apply autonamous debugging with Tregression.

# Citation
If you need to reference our technique, please use the following citations:

- Haijun Wang#, Yun Lin#*, Zijiang Yang, Jun Sun, Yang Liu, Jin Song Dong, Qinghua Zhen, and Ting Liu. Explaining Regressions via Alignment Slicing and Mending, Transcation on Software Engineering (TSE 2019). (#co-first author, *corrsponding author)

# Source Code Configuration
## Dependency
The TRegression (i.e., ERASE) project relies on Microbat project to collect execution Trace of Java program. When you are importing tregression project, you need to important Microbat project (https://github.com/llmhyy/microbat) as well. Note that all the projects are Eclipse plugin project. The imported projects are listed as follows:
- microbat (microbat)
- microbat_instrumentator (microbat)
- microbat_junit_test (microbat)
- mutation (microbat)
- sav.commons (microbat)
- tregression (tregression)

Moreover, this prototype are build on top of Defects4J bugs. We forked Defects4J repository (https://github.com/llmhyy/defects4j) and please checkout the buggy version and fixed version by our script. (https://github.com/llmhyy/defects4j/blob/master/checkout.sh). If you run the script successfully, you can checkout the bug file structure as follows:
bug_repo

|__ Chart (project_id)<br />
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|__ 1 (bug_id)<br />
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|__ 2   <br /> 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|__ ...<br />

Last, please unzip this file (https://github.com/llmhyy/tregression/blob/master/tregression/dependent_lib/junit_lib.rar) under the dropins directory of your eclipse root folder. It contains all the runtime Java libraries.

## Running Tregression (ERASE) on Defects4J bugs
After import the projects, right-click the tregression project, and choose "Run As Eclipse Application", you can start debugging Tregression. You need to configure the settings in the Eclipse-application as follows:
![Snapshot of TRregression](/tregression/icons/preference_configuration.png?raw=true "Snapshot of TRregression Settings")

Second, please switch to Tregression perspective by (Windows >> Perspectives >> Open Operspective >> Other). 

Third, click "Tregression" menu >> Run for Seperate Versions. The tool will automate the regression bug detection.

## Running Tregression (ERASE) on Regs4J bugs
It also supports Regressions4J (https://github.com/SongXueZhi/regs4j) bugs. There is no need to download Regressions4J's CLI.sh and CLI.jar, as we already provide our modified version in the scripts directory. Open and modify scripts/regs4j.sh file as specified inside. Then run regs4j.sh as instructed in the file itself. Similar to Defects4j, it will clone all buggy and working versions to your specified directory.

The regs4j.sh script also creates a CSV file that logs any error faced when cloning, for instance, unexpected test results (failing for working commit/passing for bug inducing commit), maven compilation errors, etc. The CSV file is also used to track which bugs were already cloned, so to reclone the projects, please remove the specific row in the file, or provide a different path for the CSV file.

### Notes for Developers
#### Regressions4J's CLI.jar
The current JAR file in the scripts directory is based off of Release 1.2 (https://github.com/SongXueZhi/regs4j/releases/tag/1.2).
Modifications were made to integrate with TRregression.
- example.CLI#checkout
	- It additionally clones the working commit, and migrates the test case to the working commit. Before this was only done to the RIC commit.
- core.Migrator#migrateTestFromTo_0
	- Migrated files that were new to the working/RIC commit were deleted on exit from the CLI tool, however, it is now saved. (To save migrated test files and its dependencies)

#### regs4jProjAndBugs.sh
This bash script will print out all the project names and bug ID numbers in regs4j. They can copied into tregression.constants.Dataset class if there are any updates to the database.
