import random
from collections import defaultdict
from itertools import combinations, product
from typing import List, Tuple, Dict

import metis
import networkx as nx
from shapely.geometry import Polygon
from shapely.ops import cascaded_union

from election.g6.src import dist_analysis
from election.g6.src.voter import Voter


def batch(iterable, n=1):
    l = len(iterable)
    for ndx in range(0, l, n):
        yield iterable[ndx:min(ndx + n, l)]


def find_adjacent_triangle(index, triangles: List[Dict]):
    inds = []
    tr = triangles[index]
    for i, t in enumerate(triangles):
        if i == index:
            continue
        x1, y1 = t['polygon'].exterior.coords.xy
        x2, y2 = tr['polygon'].exterior.coords.xy
        n_p = 0
        for l in range(3):
            for j in range(3):
                if x1[l] == x2[j] and y1[l] == y2[j]:
                    n_p += 1
        if n_p >= 2:
            inds.append(i)
    return inds


def is_in_polygon(voter: Voter, polygon: Polygon) -> bool:
    return polygon.contains(voter.location)