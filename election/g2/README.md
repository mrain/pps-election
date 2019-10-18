# README

## How to run and verify code
The main files are `2party1elector256districts.ipynb` and `2party1elector243districts`. The former recursively splits districts into even halves while the latter recursively splits districts into even thirds.

To test it with a different map, you'll need to modify the cell after the "District Generation Script" header.

Replace `mmap = LOADED_MAPS[1]` with `mmap = map_util.load_map(map_file)`, where `map_file` is the path to the map file you want to load. When the cell is run, it should automatically visualize the map and print voter counts so you can verify it is indeed correct.

Then simply run the following cells to generate and save districts (change last cell file location to the desired output file name).

The output file can be loaded into the simulator without issues.

# Results

The results below are preliminary based on our code as of Wednesday night. They are subject to change

## Two Land Results (Districts split by 2)

| Configuration | Districts | Party choice | 1 elector | 3 elector |
| --- | --- | --- | --- | --- |
| tour2.map | 256 districts | Party 1 | [242, 14] | |
| | | Party 2 | [18, 238] | |
| tour2.map | 64 districts | Party 1 | [64, 0] | [128, 64] |
| | | Party 2 | [3, 61] | [69, 123] |
| tour3.map | 256 districts | Party 1 | [152, 55, 49] | |
| | | Party 2 | [53, 159, 44] | |
| | | Party 3 | [53, 47, 156] | |
| tour3.map | 64 districts | Party 1 | [35, 16, 13] | [78, 59, 55] |
| | | Party 2 | [14, 38, 12] | [59, 81, 53] | 
| | | Party 3 | [13, 16, 35] | [51, 60, 81] |

## Three Land Results (Districts split by 3)

| Configuration | Districts | Party choice | 1 elector | 3 elector |
| --- | --- | --- | --- | --- |
| tour2.map | 243 districts | Party 1 | [209, 34] | |
| | | Party 2 | [19, 224] | |
| tour2.map | 81 districts | Party 1 | [80, 1] | [161, 82] |
| | | Party 2 | [3, 78] | [87, 156] |
| tour3.map | 243 districts | Party 1 | [120, 66, 57] | |
| | | Party 2 | [68, 120, 55] | |
| | | Party 3 | [65, 55, 123] | |
| tour3.map | 81 districts | Party 1 | [32, 27, 22] | [83, 82, 78] |
| | | Party 2 | [22, 34, 25] | [80, 82, 81] | 
| | | Party 3 | [23, 21, 37] | [80, 75, 88] |
