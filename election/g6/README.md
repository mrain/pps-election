## Usage

### Map generation

To generate a map, run from pps-election folder root:

```bash
python3.7 election/g6/srcipts/generatemap.py
```

If you want to modify the default parameters:

```bash
python3.7 election/g6/srcipts/generatemap.py \\
    -m election.g6.src.mapgenerator \\
    -o maps/g6/1.map \\
    --seed 42 \\
    --voters 333333 \\
    --parties 3
```

### District generation


To generate a map, run from pps-election folder root:

```bash
python3.7 election/g6/srcipts/generatedistricts.py -i maps/g7/3.map
```

If you want to modify the default parameters:

```bash
python3.7 election/g6/srcipts/generatemap.py \\
    -m election.g6.src.districtgenerator \\
    -i maps/g6/3.map \\
    --seed 42 \\
    --representatives 3
```