## Usage

To generate a map, run from pps-election folder root:

```bash
python3.7 election/g6/srcipts/generatemap.py
```

If you want to modify the default parameters:

```bash
python3.7 election/g6/srcipts/generatemap.py \\
    -m election.g1.src.mapgenerator \\
    -o maps/g1/1.map \\
    --seed 42 \\
    --voters 333333 \\
    --parties 3
```

