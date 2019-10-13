import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from PIL import Image
from IPython import display
import math
import random
from sklearn.cluster import KMeans
from shapely.geometry import Point, Polygon, LineString
from scipy.spatial import Voronoi, voronoi_plot_2d
%matplotlib inline

class Voter:
    def __init__(self, x, y, prefs):
        self.x = x
        self.y = y
        self.prefs = prefs

def extractVoters(filename):
    f = open(filename, "r")
    content = f.readlines()
    voters = list()
    for i in range(1, len(content)-1):
        line = content[i].split()
        x = line[0]
        y = line[1]
        prefs = line[2:]
        voters.append(Voter(x, y, prefs))

    return voters

def RandomVotersGenerator():
    num_Voters = 0
    voters = []
    while(num_Voters < 333333):
        x = random.uniform(0,1000)
        y = random.uniform(0, 500*math.sqrt(3))
        curr_voter = (x, y)
        if curr_voter not in voters and insideBoundary(curr_voter) == True:
            voters.append(curr_voter)
            num_Voters += 1
    return voters

def insideBoundary(voter):
    x,y = voter[0], voter[1]
    x1, y1, x2, y2, x3, y3 = 0,0,1000,0,500,500*math.sqrt(3)

    A = area (x1, y1, x2, y2, x3, y3)
    A1 = area (x, y, x2, y2, x3, y3)
    A2 = area (x1, y1, x, y, x3, y3)
    A3 = area (x1, y1, x2, y2, x, y)

    if(A == A1 + A2 + A3):
        return True
    else:
        return False

def area(x1, y1, x2, y2, x3, y3):
    return abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0)

def sample_new_point(prev_x, prev_y, area):
    def out_of_bounds(x, y):
        center = (500, 250 * np.sqrt(3))
        top = (500, 500 * math.sqrt(3))
        left = (0, 0)
        right = (1000, 0)
        line = LineString([(x, y), center])
        e1 = LineString([left, right])
        e2 = LineString([left, top])
        e3 = LineString([right, top])
        return line.intersects(e1) or line.intersects(e2) or line.intersects(e3)

    mean = [0, 0]
    cov = [[area, 0], [0, area]]
    while True:
        delta_x, delta_y = np.random.multivariate_normal(mean, cov, 1).T
        delta_x = delta_x[0]
        delta_y = delta_y[0]
        new_x, new_y = prev_x + delta_x, prev_y + delta_y
        if not out_of_bounds(new_x, new_y):
            return new_x, new_y

def asymmetry_score(districts, voters):
    results = list()
    #results is a list of tuples (party1seats, party2seats)
    for i in range(0.0, 0.5, .05):
        new_voters = copy.deepcopy(voters)
        for v in new_voters:
            v.prefs = adjust_voter_preference(v.prefs, i)
        result = get_result(districts, voters)
        results.append(result)
    results.sort()
    median = results.get(len(results)/2)
    min = results.get(0)
    max = results.get(len(results)-1)
    asymmetry_score = (max[0] - med[0]) - (med[0] - min[0])
    #asymmetry_score will be 0 for perfectly fair election
    return asymmetry_score

def net_efficiency_gap(districts, voters, results):
    # results is a map of district to vote counts
    party1_wasted = 0
    party2_wasted = 0
    for district in districts:
        wasted1, wasted2 = get_wasted_votes(district, voters)
        party1_wasted += wasted1
        party2_wasted += wasted2

    e_gap = (party1_wasted - party2_wasted) / len(voters)
    return e_gap

def get_wasted_votes(district, voters):
    print("Returns party1 wasted votes, party2 wasted votes")

def get_result(districts, voters):
    print("Returns a tuple (party1 seats, party2 seats) for the whole election")

def adjust_voter_preference(pref, target_p2=0.5):
    p1_boost = 0.5 - target_p2
    p2_boost = target_p2 - 0.5
    beta = max(0.01, p1_boost + pref[0])
    alpha = max(0.01, p2_boost + pref[1])
    p2_prob = np.random.beta(alpha, beta)
    return [1.0 - p2_prob, p2_prob]

def sample_vote(pref):
    assert sum(pref) == 1.0
    return np.random.binomial(size=1, n=1, p=pref[1])[0]

print(sample_new_point(250, 250, 100))
print(sample_vote(adjust_voter_preference([0.5, 0.5], target_p2=0.6)))

voters = extractVoters("../../maps/g8/twoParties.map")

x_list = [v.x for v in voters]
y_list = [v.y for v in voters]
X = np.zeros((len(x_list),2))
X[:,0] = np.matrix(x_list)
X[:,1] = np.matrix(y_list)

# kmeans = KMeans(n_clusters=243, init='k-means++', max_iter=300, n_init=10, random_state=0)
# pred_y = kmeans.fit_predict(X)
# #plt.scatter(X[:,0], X[:,1], s=10, edgecolors='none', c='green')
# plt.scatter(kmeans.cluster_centers_[:, 0], kmeans.cluster_centers_[:, 1], s=10, c='red')
# plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
# plt.show()
#
# v = Voronoi(kmeans.cluster_centers_)
# voronoi_plot_2d(v, show_vertices=False)
# plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
# plt.xlim(0,1000)
# plt.ylim(0,1000)
#
# # Load Data
# file_path = 'coordinates.txt'
# data = np.genfromtxt(file_path,dtype="i4,i4,U1",
# delimiter=',',names=['x1','x2','class'])
# print(data.shape)
#
# V = np.vstack([np.array((x[0],x[1])) for x in data])
# np.random.shuffle(V)
# V = V[0:333333,:]
#
# kmeans = KMeans(n_clusters=243, init='k-means++', max_iter=300, n_init=10, random_state=0)
# pred_y = kmeans.fit_predict(V)
# plt.scatter(kmeans.cluster_centers_[:, 0], kmeans.cluster_centers_[:, 1], s=10, c='red')
# plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
# plt.show()
#
# v = Voronoi(kmeans.cluster_centers_)
# voronoi_plot_2d(v, show_vertices=False)
# plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
# plt.xlim(0,1000)
# plt.ylim(0,1000)
