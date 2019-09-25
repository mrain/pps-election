# Project 2: Election
Course: COMS 4444 Programming and Problem Solving (F2019)  
Uni: Columbia University  
Instructor: Prof. Kenneth Ross   
TAs: Vaibhav Darbari, Chengyu Lin   

# User Guide
First thing you need to know is our GUI `statics/index.html`. You may directly open it and
load your file to visualize your result. 2 sample maps and 1 sample result file are provided.

In this project, you can implement your ideas using other languages and
use this library to verify your result. If you choose our platform,
you need to implement the following interfaces:
* `election.sim.MapGenerator`
* `election.sim.DistrictGenerator`

You may refer to files in `election/g0` for details.

Following 2 modules are used to run/verify your result:
* `election.sim.MapUtils`
  * You may use to run your programed module:
  ```bash
  $ java election.sim.MapUtils run election.g0.RandomMapGenerator \
         -m maps/random3.map \
         -n 333333 -p 3 --seed 20190925
  ```
  * Also you may use it to validate your generated map file
  ```bash
  $ java election.sim.MapUtils verify -m maps/random3.map
  ```
* `election.sim.Run`
  * Similar useage here:
  ```bash
  $ java election.sim.Run run election.g0.DistrictGenerator \
         -m maps/random3.map \
         -r result.dat --seed 20190925 -rep 3
  ```
  * Also you may use it to validate your generated map file
  ```bash
  $ java election.sim.Run verify -r result.dat
  ```

# Map File Specification
 * The board is a giant equilateral triangle with edge length 1000.
    * Three vertices are `(0, 0)`, `(0, 1000)` and `(500, 500*sqrt(3))`.
 * First line contains 2 integers: `numVoters` and `numParties`.
   They correspond to the number of voters and the number of parties, respectively.
 * The following `numVoters` lines contains the information for each voter.
    * Each line there are `numParties + 2` float numbers.
      First 2 correspond to the location of this voter.
      Later `numParties` numbers are his/her preferences.
 * After `numVoters + 1` lines, there shall be a line containing `0` only.

 # Result File Specification
 * First `numVoters + 1` lines are exactly the same with the map file.
 * Starting from `numVoters + 2` line, it contains a single integer `numDistricts`
   corresponding to the number of districts.
 * The following `numDistricts` lines describe the shape of districts.
    * Each line starts with an integer `p`, followed by `2p` float numbers describes the
      coordinates of `p` vertices of this district (a polygon).
      The `p` vertices shall be given in order, either clockwise or counter-clockwise.
