import os

seed = 20191019
module = 'election.g9.DistrictGenerator'
maps_dir = "tournaments/maps/"
results_dir = "tournaments/g9/"
maps = []

for (root, dirs, files) in os.walk(maps_dir):
    maps.extend([f for f in files if not f[0] == '.'])

for f in maps:
    map_path = os.path.join(maps_dir, f)
    for rep in [1,3]:
        result_path = os.path.join(results_dir, f[:-4] + "-" + str(rep) + '.dat')
        log_path = os.path.join(results_dir, f[:-4] + "-" + str(rep) + '.log')
        err_path = os.path.join(results_dir, f[:-4] + "-" + str(rep) + '.err')
        cmd = 'java election.sim.Run run {} -m {} -r {} --seed {} -rep {} -tl 600000 > {} 2> {}'.format(module, map_path, result_path, seed, rep, log_path, err_path)
        print(cmd)
        os.system(cmd)

