# Map File Specification
 * The board is a giant equilateral triangle with edge length 1000.
    * Three vertices are `(0, 0)`, `(1000, 0)` and `(500, 500*sqrt(3))`.
 * First line contains 2 integers: `numVoters` and `numParties`. They correspond to the number of voters and the number of parties, respectively.
 * The following `numVoters` lines contains the information for each voter.
    * Each line there are `numParties + 2` floating numbers. First 2 correspond to the location of this voter. Later `numParties` numbers are his/her preferences.
 * After `numVoters` lines, there shall be a line containing `0` only.
