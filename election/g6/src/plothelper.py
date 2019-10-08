import math
import random
from typing import List

import numpy as np
from shapely.geometry import Point, Polygon

from election.g6.src.mapgenerator import get_normal, triangle

import matplotlib.pyplot as plt

total_population = 10000
voters = get_normal(total_population//2, 300, 300, 60, 40) + get_normal(total_population//2, 700, 500, 60, 100)

import matplotlib.pyplot as plt
def draw_polygons(polygons: List[Polygon]):
    for polygon in polygons:
        plt.plot(*polygon.exterior.xy)
        
def draw_points(points):
    points = [point for line in points for point in line]
    x, y = zip(*points)
    plt.scatter(x, y)
    
def draw_voters(voters, draw_triangle=True):
    if draw_triangle: plt.plot(*triangle.exterior.xy)
    if not voters: return
    points = [(voter.location.x, voter.location.y) for voter in voters]
    x, y = zip(*points)
    plt.scatter(x, y, s=10)