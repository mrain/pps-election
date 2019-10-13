import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from PIL import Image
from IPython import display
import math
import random
from sklearn.cluster import KMeans
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

x_list = [x for (x,y) in voters]
y_list = [y for (x,y) in voters]
X = np.zeros((len(x_list),2))
X[:,0] = np.matrix(x_list)
X[:,1] = np.matrix(y_list)

kmeans = KMeans(n_clusters=243, init='k-means++', max_iter=300, n_init=10, random_state=0)
pred_y = kmeans.fit_predict(X)
#plt.scatter(X[:,0], X[:,1], s=10, edgecolors='none', c='green')
plt.scatter(kmeans.cluster_centers_[:, 0], kmeans.cluster_centers_[:, 1], s=10, c='red')
plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
plt.show()

v = Voronoi(kmeans.cluster_centers_)
voronoi_plot_2d(v, show_vertices=False)
plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
plt.xlim(0,1000)
plt.ylim(0,1000)

# Load Data
file_path = 'coordinates.txt'
data = np.genfromtxt(file_path,dtype="i4,i4,U1",
delimiter=',',names=['x1','x2','class'])
print(data.shape)

V = np.vstack([np.array((x[0],x[1])) for x in data])
np.random.shuffle(V)
V = V[0:333333,:]

kmeans = KMeans(n_clusters=243, init='k-means++', max_iter=300, n_init=10, random_state=0)
pred_y = kmeans.fit_predict(V)
plt.scatter(kmeans.cluster_centers_[:, 0], kmeans.cluster_centers_[:, 1], s=10, c='red')
plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
plt.show()

v = Voronoi(kmeans.cluster_centers_)
voronoi_plot_2d(v, show_vertices=False)
plt.plot([0,1000,500,0],[0,0,500*math.sqrt(3),0])
plt.xlim(0,1000)
plt.ylim(0,1000)
