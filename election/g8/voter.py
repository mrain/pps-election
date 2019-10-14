import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import math
import copy
import random
from sklearn.cluster import KMeans
from sklearn.cluster import MiniBatchKMeans
from shapely.geometry import Point, Polygon, LineString
from scipy.spatial import Voronoi, voronoi_plot_2d

WINNER_TAKE_ALL = False
NUM_VOTERS = 30000


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
        x = float(line[0])
        y = float(line[1])
        prefs = list(map(float, line[2:]))
        voters.append(Voter(x, y, prefs))

    return voters


def RandomVotersGenerator():
    num_Voters = 0
    voters = []
    while(num_Voters < NUM_VOTERS):
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
    ct = 0
    while True:
        delta_x, delta_y = np.random.multivariate_normal(mean, cov, 1).T
        delta_x = delta_x[0]
        delta_y = delta_y[0]
        new_x, new_y = prev_x + delta_x, prev_y + delta_y
        if not out_of_bounds(new_x, new_y):
            return new_x, new_y
        ct += 1
        if ct > 10:
            # TODO fix.  Some points are out of bounds before sampling
            return new_x, new_y


def asymmetry_score(districts, voters):
    seats_by_vote_perc = {}
    total_wasted_votes = np.zeros([2, ])
    variations = np.arange(0.25, 1.0, .25)
    for target_v in variations:
        new_voters = copy.deepcopy(voters)
        for v in new_voters:
            v.prefs = adjust_voter_preference(v.prefs, target_p2=target_v)
        popular_vote, seats, wasted_votes = get_result(districts, new_voters)
        p2_vote_perc = popular_vote[1] / float(len(voters))
        seats_by_vote_perc[p2_vote_perc] = seats[1] / 243.0
        total_wasted_votes += np.array(wasted_votes)
    avg_wasted_votes = total_wasted_votes / float(len(variations))
    avg_efficiency_gap = (avg_wasted_votes[0] - avg_wasted_votes[1]) / float(len(voters))
    avg_pref_variation = np.mean(np.array(list(seats_by_vote_perc.keys())))
    assert avg_pref_variation > 0.45 and avg_pref_variation < 0.55
    avg_votes_to_seats = np.mean(np.array(seats_by_vote_perc.values()))
    avg_votes_to_seats_norm = 2 * avg_votes_to_seats - 1
    return (avg_votes_to_seats_norm + avg_efficiency_gap) / 0.5


def find_voter_district(districts, voter, recent_district_idxs=[]):
    p = Point(voter.x, voter.y)
    for idx in recent_district_idxs:
        if districts[idx].contains(p):
            return idx
    for idx, district in enumerate(districts):
        if district.contains(p):
            return idx


def compute_seat_count(party_votes):
    if sum(party_votes) == 0:
        # TODO fix this (shouldn't have no voters in a district)
        if random() > 0.5:
            return [2, 1]
        else:
            return [1, 2]

    p1_pref = party_votes[0] / float(sum(party_votes))
    p2_pref = party_votes[1] / float(sum(party_votes))
    if WINNER_TAKE_ALL:
        if p1_pref > p2_pref:
            return [3, 0]
        else:
            return [0, 3]
    p1_seats, p2_seats = 0, 0
    while p1_seats + p2_seats < 3:
        if p1_pref > p2_pref:
            p1_seats += 1
            p1_pref -= .25
        else:
            p2_seats += 1
            p2_pref -= .25
    return (p1_seats, p1_pref), (p2_seats, p2_pref)


def compute_seats(district_votes):
    seats = np.zeros([2, ])
    wasted_votes = np.zeros([2, ])
    for dv in district_votes:
        (p1_seats, p1_pref), (p2_seats, p2_pref) = compute_seat_count(dv)
        seats[0] += p1_seats
        seats[1] += p2_seats
        wasted_votes[0] += p1_pref
        wasted_votes[1] += p2_pref
    return seats, wasted_votes


def get_result(districts, voters):
    district_votes = np.zeros([len(districts), 2])
    last_districts = []
    for voter in voters:
        district_idx = find_voter_district(districts, voter, last_districts)
        if district_idx not in last_districts:
            last_districts.append(district_idx)
            if len(last_districts) > 3:
                last_districts = last_districts[1:]
        vote = sample_vote(voter.prefs)
        district_votes[district_idx, vote] += 1
    popular_vote = district_votes.sum(0)
    seats, wasted_votes = compute_seats(district_votes)
    return popular_vote, seats, wasted_votes


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


def is_valid_draw(new_districts, voters):
    district_voters = np.zeros([len(districts)])
    last_districts = []
    for voter in voters:
        district_idx = find_voter_district(new_districts, voter, last_districts)
        if district_idx not in last_districts:
            last_districts.append(district_idx)
            if len(last_districts) > 3:
                last_districts = last_districts[1:]
        district_voters[district_idx] += 1
    N = float(len(voters))
    mean = N / float(len(new_districts))
    lower = mean * 0.9
    upper = mean * 1.1
    for idx in range(len(new_districts)):
        if district_voters[idx] < lower or district_voters[idx] > upper:
            return False
    return True


def sample_new_district_centers(centroids, districts, voters):
    new_centroids = np.zeros([len(centroids), 2])
    for idx, (centroid, district) in enumerate(zip(centroids, districts)):
        new_pt = sample_new_point(centroid[0], centroid[1], np.sqrt(district.area))
        new_centroids[idx, :] = new_pt
    new_districts = draw_districts(new_centroids)
    if not is_valid_draw(new_districts, voters):
        return sample_new_district_centers(districts, voters)
    return new_centroids, new_districts


# Clip the Voronoi Diagram
# Run "conda install shapely -c conda-forge" on terminal first
# Method from StackOverflow
# Reference : https://stackoverflow.com/questions/36063533/clipping-a-voronoi-diagram-python
def voronoi_finite_polygons_2d(vor, radius=None):
    """
    Reconstruct infinite voronoi regions in a 2D diagram to finite
    regions.
    Parameters
    ----------
    vor : Voronoi
        Input diagram
    radius : float, optional
        Distance to 'points at infinity'.
    Returns
    -------
    regions : list of tuples
        Indices of vertices in each revised Voronoi regions.
    vertices : list of tuples
        Coordinates for revised Voronoi vertices. Same as coordinates
        of input vertices, with 'points at infinity' appended to the
        end.
    """

    if vor.points.shape[1] != 2:
        raise ValueError("Requires 2D input")

    new_regions = []
    new_vertices = vor.vertices.tolist()

    center = vor.points.mean(axis=0)
    if radius is None:
        radius = vor.points.ptp().max()*2

    # Construct a map containing all ridges for a given point
    all_ridges = {}
    for (p1, p2), (v1, v2) in zip(vor.ridge_points, vor.ridge_vertices):
        all_ridges.setdefault(p1, []).append((p2, v1, v2))
        all_ridges.setdefault(p2, []).append((p1, v1, v2))

    # Reconstruct infinite regions
    for p1, region in enumerate(vor.point_region):
        vertices = vor.regions[region]

        if all(v >= 0 for v in vertices):
            # finite region
            new_regions.append(vertices)
            continue

        # reconstruct a non-finite region
        ridges = all_ridges[p1]
        new_region = [v for v in vertices if v >= 0]

        for p2, v1, v2 in ridges:
            if v2 < 0:
                v1, v2 = v2, v1
            if v1 >= 0:
                # finite ridge: already in the region
                continue

            # Compute the missing endpoint of an infinite ridge

            t = vor.points[p2] - vor.points[p1] # tangent
            t /= np.linalg.norm(t)
            n = np.array([-t[1], t[0]])  # normal

            midpoint = vor.points[[p1, p2]].mean(axis=0)
            direction = np.sign(np.dot(midpoint - center, n)) * n
            far_point = vor.vertices[v2] + direction * radius

            new_region.append(len(new_vertices))
            new_vertices.append(far_point.tolist())

        # sort region counterclockwise
        vs = np.asarray([new_vertices[v] for v in new_region])
        c = vs.mean(axis=0)
        angles = np.arctan2(vs[:,1] - c[1], vs[:,0] - c[0])
        new_region = np.array(new_region)[np.argsort(angles)]

        # finish
        new_regions.append(new_region.tolist())

    return new_regions, np.asarray(new_vertices)


def draw_districts(centroids):
    vor = Voronoi(centroids)
    voronoi_plot_2d(vor, show_vertices=False)
    regions, vertices = voronoi_finite_polygons_2d(vor)

    # Box the triangular boundary
    box = Polygon([[0, 0], [1000, 0], [500, 500*math.sqrt(3)]])

    # Final Output Districts
    districts = []
    # Colorize Districts
    for region in regions:
        polygon = vertices[region]
        # Clipping polygon
        poly = Polygon(polygon)
        poly = poly.intersection(box)
        districts.append(poly)
    return districts


if __name__ == '__main__':
    # Load voter data
    # Note : The data in coordinate.txt has some error because some points outside the triangular boundary.
    file_path = '../../maps/g9/coordinates.txt'
    data = np.genfromtxt(file_path,dtype="i4,i4,U1", delimiter=',',names=['x1','x2','class'])
    # Extract Voters Positions
    V = np.vstack([np.array((x[0],x[1])) for x in data])
    # Shuffle the Voters Positions and Randomly Select 333333 Voters
    np.random.shuffle(V)
    V = V[0:NUM_VOTERS, :]
    kmeans = MiniBatchKMeans(n_clusters=81, random_state=0, batch_size=32, max_iter=5, init_size=3 * 81).fit(V)

    # Generate Voronoi with generator points = cluster centroids
    # Note : Some generator points outside tringular boundary due to the error in coordinates.txt data
    centroids = kmeans.cluster_centers_
    districts = draw_districts(centroids)

    # LOAD INITIAL DISTRICTS
    initial_districts = copy.deepcopy(districts)  # Keep a hardcopy of this

    # LOAD voters
    voters = extractVoters("../../maps/g8/twoParties.map")

    best_score = -1
    for mut_idx in range(1000):
        # Randomly jiggle map N times
        all_candidate_districts = []
        all_candidate_centroids = []
        gerrymander_scores = []
        N = 10
        for n in range(N):
            candidate_centroids, candidate_districts = sample_new_district_centers(centroids, districts, voters)
            all_candidate_districts.append(candidate_districts)
            all_candidate_centroids.append(candidate_centroids)
            gerrymander_score = asymmetry_score(candidate_districts, voters)
            gerrymander_scores.append(gerrymander_score)

        gerrymander_scores = np.array(gerrymander_scores)
        best_idx = np.argsort(gerrymander_scores)[-1]
        centroids = all_candidate_centroids[best_idx]
        best_score = max(best_score, gerrymander_scores[best_idx])
        # Choose best district from gerrymandering perspective
        districts = all_candidate_districts[best_idx]

    print('Best gerrymander score (-1, 1) is {}'.format(len(districts)))

# x_list = [v.x for v in voters]
# y_list = [v.y for v in voters]
# X = np.zeros((len(x_list),2))
# X[:,0] = np.matrix(x_list)
# X[:,1] = np.matrix(y_list)

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
