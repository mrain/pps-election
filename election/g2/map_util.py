import os
import glob
import numpy as np
import matplotlib.pyplot as plt

BASE_DIR = os.getcwd()
MAP_DIRS = os.path.abspath(os.path.join(BASE_DIR, "../../maps"))

# # Map File Specification
# - The board is a giant equilateral triangle with edge length 1000.
#     - Three vertices are `(0, 0)`, `(1000, 0)` and `(500, 500*sqrt(3))`.
# - First line contains 2 integers: `numVoters` and `numParties`. They correspond to the number of voters and the number of parties, respectively.
# - The following `numVoters` lines contains the information for each voter.
#     - Each line there are `numParties + 2` floating numbers. First 2 correspond to the location of this voter. Later `numParties` numbers are his/her preferences.
# - After `numVoters` lines, there shall be a line containing `0` only.

def list_maps(map_dir=MAP_DIRS):
    return glob.glob(map_dir+"/**/*.map", recursive=True)

def load_map(filename):
    return np.loadtxt(filename, skiprows=1, max_rows=333333)

def visualize_map(_map, ax=None, color_scheme='argmax'):
    if ax is None:
        fig, ax = plt.subplots(1, 1, figsize=(10,10))
    
    if color_scheme == 'argmax':
        colors = np.argmax(_map[:, 2:], 1)
    else:
        colors = [tuple(dist) for dist in _map[:, 2:]]

    plot = ax.scatter(_map[:,0], _map[:,1], s=0.01, c=colors, alpha=0.5)

if __name__ == '__main__':
    print(list_maps())
    
    # To use, clone class repo to this folder first
    g2_map_2 = load_map(os.path.join(MAP_DIRS, 'g2/2party.map'))
    visualize_map(g2_map_2)