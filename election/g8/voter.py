import numpy as np
from collections import defaultdict
import math
import numpy as np
import copy
import json
import random
from sklearn.cluster import MiniBatchKMeans
from sklearn.neighbors import KDTree
from shapely.geometry import Point, Polygon, LineString
from scipy.spatial import Voronoi


WINNER_TAKE_ALL = False
NUM_VOTERS = 33333
RANDOM_SEED = 1992
LOAD_CLUSTERS = False
BASIC_KMEANS = True
np.random.seed(RANDOM_SEED)


def BalancedClustering(k, cluster_centers, points):
    X = points.copy()
    centroids = np.zeros((k,2))
    for i in range(k):
        n = int(points.shape[0]/k)
        tree = KDTree(X)
        nearest_dist, nearest_ind = tree.query([cluster_centers[i,:]], k=n)
        centroids[i,:] = np.mean(X[nearest_ind[0]],axis=0)
        X = np.delete(X, nearest_ind[0], axis=0)
    return centroids


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


def sample_new_point(prev_x, prev_y, area):
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


def asymmetry_score(districts, voters, voters_by_district):
    seats_by_vote_perc = {}
    total_wasted_votes = np.zeros([2, ])
    variations = np.arange(0.1, 1.0, .1)
    for target_v in variations:
        new_voters = copy.deepcopy(voters)
        for v in new_voters:
            v.prefs = adjust_voter_preference(v.prefs, target_p2=target_v)
        popular_vote, seats, wasted_votes = get_result(districts, new_voters, voters_by_district)
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
    p1_pref = party_votes[0] / float(sum(party_votes))
    p2_pref = party_votes[1] / float(sum(party_votes))
    if WINNER_TAKE_ALL:
        if p1_pref > p2_pref:
            return [1, 0]
        else:
            return [0, 1]
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


def get_result(districts, voters, voters_by_district):
    district_votes = np.zeros([len(districts), 2])
    for didx in voters_by_district:
        voter_idxs = voters_by_district[didx]
        for vidx in voter_idxs:
            vote = sample_vote(voters[vidx].prefs)
            district_votes[didx, vote] += 1
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
    voters_by_district = defaultdict(list)
    last_districts = []
    N = float(len(voters))
    mean = N / float(len(new_districts))
    lower = mean * 0.85
    upper = mean * 1.15
    for vidx, voter in enumerate(voters):
        district_idx = find_voter_district(new_districts, voter, last_districts)
        voters_by_district[district_idx].append(vidx)
        if district_idx not in last_districts:
            last_districts.append(district_idx)
            if len(last_districts) > 3:
                last_districts = last_districts[1:]
        district_voters[district_idx] += 1

    district_voters = np.array(district_voters)
    sorted_pop_idxs = np.argsort(district_voters)
    district_voters_sorted = district_voters[sorted_pop_idxs]

    too_small_breakpoint = 999
    too_big_breakpoint = 999
    for didx, district_votes in enumerate(district_voters_sorted):
        if district_votes > lower:
            too_small_breakpoint = min(too_small_breakpoint, didx)
        if district_votes > upper:
            too_big_breakpoint = min(too_big_breakpoint, didx)

    too_big_district_idxs = []
    too_small_district_idxs = []
    if too_small_breakpoint < 999:
        too_small_district_idxs = sorted_pop_idxs[:too_small_breakpoint]
    if too_big_breakpoint < 999:
        too_big_district_idxs = sorted_pop_idxs[too_big_breakpoint:]

    if len(too_small_district_idxs) + len(too_big_district_idxs) == 0:
        return True, voters_by_district, 0

    underflow = lower - district_voters[too_small_district_idxs]
    overflow = district_voters[too_big_district_idxs] - upper

    total_overflow = overflow.sum()
    total_underflow = underflow.sum()

    print('Total Underflow / Overflow is {} / {} voters'.format(total_underflow, total_overflow))
    return False, None, total_overflow + total_underflow


def find_closest(centroids, idx, n=2):
    distances = []
    for cidx, centroid in enumerate(centroids):
        if cidx == idx:
            distance = 999999999
        else:
            distance = np.sqrt(np.power(centroid[0] - centroids[idx][0], 2) +
                               np.power(centroid[1] - centroids[idx][1], 2))

        distances.append(distance)

    return np.argsort(np.array(distances))[:n]


def sample_new_district_centers(centroids, districts, voters, sample=True):
    if sample:
        new_centroids = np.zeros([len(centroids), 2])
        for idx, (centroid, district) in enumerate(zip(centroids, districts)):
            new_pt = sample_new_point(centroid[0], centroid[1], np.sqrt(district.area))
            new_centroids[idx, :] = new_pt
        new_districts = draw_districts(new_centroids)
    else:
        new_centroids = centroids
        new_districts = districts
    is_valid, voters_by_district, prev_total_overflow = is_valid_draw(new_districts, voters)
    iteration = 0
    while not is_valid:
        centroid_candidates = new_centroids.copy()

        NR = 1
        for i in range(NR):
            didx = np.random.choice(np.arange(len(centroid_candidates)))
            centroid_candidates[didx][0] = centroid_candidates[didx][0] + np.random.normal(0, 5 / float(NR))
            centroid_candidates[didx][1] = centroid_candidates[didx][1] + np.random.normal(0, 5 / float(NR))

        district_candidates = draw_districts(centroid_candidates)
        is_valid, voters_by_district, total_flow = is_valid_draw(district_candidates, voters)
        if total_flow <= prev_total_overflow or (total_flow < 1 and random() > 0.95):
            prev_total_overflow = total_flow
            new_districts = district_candidates
            new_centroids = centroid_candidates
        else:
            print('Tried unsuccessfully')

        if total_flow < 1.0 and iteration > 10000:
            print('Saving almost data!')
            json.dump(new_centroids.tolist(), open('adjusted_data/almost_centroids.npy', 'w'))
            np.save(open('adjusted_data/almost_districts.npy', 'wb'), new_districts)
        iteration += 1
    return new_centroids, new_districts, voters_by_district


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
    # voronoi_plot_2d(vor, show_vertices=False)
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

"""Equal Groups K-Means clustering utlizing the scikit-learn api and related
utilities.

BSD 3-clause "New" or "Revised" License

version 0.17.1
"""


import warnings

import numpy as np
import scipy.sparse as sp

from sklearn.base import BaseEstimator, ClusterMixin, TransformerMixin
from sklearn.cluster import k_means_
from sklearn.cluster import _k_means
from sklearn.externals.joblib import Parallel
from sklearn.externals.joblib import delayed
from sklearn.metrics.pairwise import euclidean_distances
from sklearn.utils.extmath import row_norms, squared_norm
from sklearn.utils.sparsefuncs import mean_variance_axis
from sklearn.utils import check_array
from sklearn.utils import check_random_state
from sklearn.utils import as_float_array
from sklearn.utils.validation import check_is_fitted
from sklearn.utils.validation import FLOAT_DTYPES


class EqualGroupsKMeans(BaseEstimator, ClusterMixin, TransformerMixin):
    """Equal Groups K-Means clustering

    90 percent of this is the Kmeans implmentations with the equal groups logic
    located in `_labels_inertia_precompute_dense()` which follows the steps laid
    out in the Elki Same-size k-Means Variation tutorial.

    https://elki-project.github.io/tutorial/same-size_k_means

    Please note that this implementation only works in scikit-learn 17.X as later
    versions having breaking changes to this implementation.

    Parameters
    ----------
    n_clusters : int, optional, default: 8
        The number of clusters to form as well as the number of
        centroids to generate.
    max_iter : int, default: 300
        Maximum number of iterations of the k-means algorithm for a
        single run.
    n_init : int, default: 10
        Number of time the k-means algorithm will be run with different
        centroid seeds. The final results will be the best output of
        n_init consecutive runs in terms of inertia.
    init : {'k-means++', 'random' or an ndarray}
        Method for initialization, defaults to 'k-means++':
        'k-means++' : selects initial cluster centers for k-mean
        clustering in a smart way to speed up convergence. See section
        Notes in k_init for more details.
        'random': choose k observations (rows) at random from data for
        the initial centroids.
        If an ndarray is passed, it should be of shape (n_clusters, n_features)
        and gives the initial centers.
    precompute_distances : {'auto', True, False}
        Precompute distances (faster but takes more memory).
        'auto' : do not precompute distances if n_samples * n_clusters > 12
        million. This corresponds to about 100MB overhead per job using
        double precision.
        True : always precompute distances
        False : never precompute distances
    tol : float, default: 1e-4
        Relative tolerance with regards to inertia to declare convergence
    n_jobs : int
        The number of jobs to use for the computation. This works by computing
        each of the n_init runs in parallel.
        If -1 all CPUs are used. If 1 is given, no parallel computing code is
        used at all, which is useful for debugging. For n_jobs below -1,
        (n_cpus + 1 + n_jobs) are used. Thus for n_jobs = -2, all CPUs but one
        are used.
    random_state : integer or numpy.RandomState, optional
        The generator used to initialize the centers. If an integer is
        given, it fixes the seed. Defaults to the global numpy random
        number generator.
    verbose : int, default 0
        Verbosity mode.
    copy_x : boolean, default True
        When pre-computing distances it is more numerically accurate to center
        the data first.  If copy_x is True, then the original data is not
        modified.  If False, the original data is modified, and put back before
        the function returns, but small numerical differences may be introduced
        by subtracting and then adding the data mean.
    Attributes
    ----------
    cluster_centers_ : array, [n_clusters, n_features]
        Coordinates of cluster centers
    labels_ :
        Labels of each point
    inertia_ : float
        Sum of distances of samples to their closest cluster center.
    Notes
    ------
    The k-means problem is solved using Lloyd's algorithm.
    The average complexity is given by O(k n T), were n is the number of
    samples and T is the number of iteration.
    The worst case complexity is given by O(n^(k+2/p)) with
    n = n_samples, p = n_features. (D. Arthur and S. Vassilvitskii,
    'How slow is the k-means method?' SoCG2006)
    In practice, the k-means algorithm is very fast (one of the fastest
    clustering algorithms available), but it falls in local minima. That's why
    it can be useful to restart it several times.
    See also
    --------
    MiniBatchKMeans:
        Alternative online implementation that does incremental updates
        of the centers positions using mini-batches.
        For large scale learning (say n_samples > 10k) MiniBatchKMeans is
        probably much faster to than the default batch implementation.
    """

    def __init__(self, n_clusters=81, init='k-means++', n_init=10, max_iter=100,
                 tol=1e-4, precompute_distances='auto', verbose=1, random_state=None, copy_x=True, n_jobs=-1):
        self.n_clusters = n_clusters
        self.init = init
        self.max_iter = max_iter
        self.tol = tol
        self.precompute_distances = precompute_distances
        self.n_init = n_init
        self.verbose = verbose
        self.random_state = random_state
        self.copy_x = copy_x
        self.n_jobs = n_jobs

    def _check_fit_data(self, X):
        """Verify that the number of samples given is larger than k"""
        X = check_array(X, accept_sparse='csr', dtype=np.float64)
        if X.shape[0] < self.n_clusters:
            raise ValueError("n_samples=%d should be >= n_clusters=%d" % (
                X.shape[0], self.n_clusters))
        return X

    def _check_test_data(self, X):
        X = check_array(X, accept_sparse='csr', dtype=FLOAT_DTYPES,
                        warn_on_dtype=True)
        n_samples, n_features = X.shape
        expected_n_features = self.cluster_centers_.shape[1]
        if not n_features == expected_n_features:
            raise ValueError("Incorrect number of features. "
                             "Got %d features, expected %d" % (
                                 n_features, expected_n_features))

        return X

    def fit(self, X, y=None):
        """Compute k-means clustering.
        Parameters
        ----------
        X : array-like or sparse matrix, shape=(n_samples, n_features)
        """
        random_state = check_random_state(self.random_state)
        X = self._check_fit_data(X)

        self.cluster_centers_, self.labels_, self.inertia_, self.n_iter_ = \
            k_means(
                X, n_clusters=self.n_clusters, init=self.init,
                n_init=self.n_init, max_iter=self.max_iter,
                verbose=self.verbose, return_n_iter=True,
                precompute_distances=self.precompute_distances,
                tol=self.tol, random_state=random_state, copy_x=self.copy_x,
                n_jobs=self.n_jobs)
        return self

    def fit_predict(self, X, y=None):
        """Compute cluster centers and predict cluster index for each sample.
        Convenience method; equivalent to calling fit(X) followed by
        predict(X).
        """
        return self.fit(X).labels_

    def fit_transform(self, X, y=None):
        """Compute clustering and transform X to cluster-distance space.
        Equivalent to fit(X).transform(X), but more efficiently implemented.
        """
        # Currently, this just skips a copy of the data if it is not in
        # np.array or CSR format already.
        # XXX This skips _check_test_data, which may change the dtype;
        # we should refactor the input validation.
        X = self._check_fit_data(X)
        return self.fit(X)._transform(X)

    def transform(self, X, y=None):
        """Transform X to a cluster-distance space.
        In the new space, each dimension is the distance to the cluster
        centers.  Note that even if X is sparse, the array returned by
        `transform` will typically be dense.
        Parameters
        ----------
        X : {array-like, sparse matrix}, shape = [n_samples, n_features]
            New data to transform.
        Returns
        -------
        X_new : array, shape [n_samples, k]
            X transformed in the new space.
        """
        check_is_fitted(self, 'cluster_centers_')

        X = self._check_test_data(X)
        return self._transform(X)

    def _transform(self, X):
        """guts of transform method; no input validation"""
        return euclidean_distances(X, self.cluster_centers_)

    def predict(self, X):
        """Predict the closest cluster each sample in X belongs to.
        In the vector quantization literature, `cluster_centers_` is called
        the code book and each value returned by `predict` is the index of
        the closest code in the code book.
        Parameters
        ----------
        X : {array-like, sparse matrix}, shape = [n_samples, n_features]
            New data to predict.
        Returns
        -------
        labels : array, shape [n_samples,]
            Index of the cluster each sample belongs to.
        """
        check_is_fitted(self, 'cluster_centers_')

        X = self._check_test_data(X)
        x_squared_norms = row_norms(X, squared=True)
        return _labels_inertia(X, x_squared_norms, self.cluster_centers_)[0]

    def score(self, X, y=None):
        """Opposite of the value of X on the K-means objective.
        Parameters
        ----------
        X : {array-like, sparse matrix}, shape = [n_samples, n_features]
            New data.
        Returns
        -------
        score : float
            Opposite of the value of X on the K-means objective.
        """
        check_is_fitted(self, 'cluster_centers_')

        X = self._check_test_data(X)
        x_squared_norms = row_norms(X, squared=True)
        return -_labels_inertia(X, x_squared_norms, self.cluster_centers_)[1]



def k_means(X, n_clusters, init='k-means++', precompute_distances='auto',
            n_init=10, max_iter=300, verbose=False,
            tol=1e-4, random_state=None, copy_x=True, n_jobs=1,
            return_n_iter=False):
    """K-means clustering algorithm.
    Read more in the :ref:`User Guide <k_means>`.
    Parameters
    ----------
    X : array-like or sparse matrix, shape (n_samples, n_features)
        The observations to cluster.
    n_clusters : int
        The number of clusters to form as well as the number of
        centroids to generate.
    max_iter : int, optional, default 300
        Maximum number of iterations of the k-means algorithm to run.
    n_init : int, optional, default: 10
        Number of time the k-means algorithm will be run with different
        centroid seeds. The final results will be the best output of
        n_init consecutive runs in terms of inertia.
    init : {'k-means++', 'random', or ndarray, or a callable}, optional
        Method for initialization, default to 'k-means++':
        'k-means++' : selects initial cluster centers for k-mean
        clustering in a smart way to speed up convergence. See section
        Notes in k_init for more details.
        'random': generate k centroids from a Gaussian with mean and
        variance estimated from the data.
        If an ndarray is passed, it should be of shape (n_clusters, n_features)
        and gives the initial centers.
        If a callable is passed, it should take arguments X, k and
        and a random state and return an initialization.
    precompute_distances : {'auto', True, False}
        Precompute distances (faster but takes more memory).
        'auto' : do not precompute distances if n_samples * n_clusters > 12
        million. This corresponds to about 100MB overhead per job using
        double precision.
        True : always precompute distances
        False : never precompute distances
    tol : float, optional
        The relative increment in the results before declaring convergence.
    verbose : boolean, optional
        Verbosity mode.
    random_state : integer or numpy.RandomState, optional
        The generator used to initialize the centers. If an integer is
        given, it fixes the seed. Defaults to the global numpy random
        number generator.
    copy_x : boolean, optional
        When pre-computing distances it is more numerically accurate to center
        the data first.  If copy_x is True, then the original data is not
        modified.  If False, the original data is modified, and put back before
        the function returns, but small numerical differences may be introduced
        by subtracting and then adding the data mean.
    n_jobs : int
        The number of jobs to use for the computation. This works by computing
        each of the n_init runs in parallel.
        If -1 all CPUs are used. If 1 is given, no parallel computing code is
        used at all, which is useful for debugging. For n_jobs below -1,
        (n_cpus + 1 + n_jobs) are used. Thus for n_jobs = -2, all CPUs but one
        are used.
    return_n_iter : bool, optional
        Whether or not to return the number of iterations.
    Returns
    -------
    centroid : float ndarray with shape (k, n_features)
        Centroids found at the last iteration of k-means.
    label : integer ndarray with shape (n_samples,)
        label[i] is the code or index of the centroid the
        i'th observation is closest to.
    inertia : float
        The final value of the inertia criterion (sum of squared distances to
        the closest centroid for all observations in the training set).
    best_n_iter: int
        Number of iterations corresponding to the best results.
        Returned only if `return_n_iter` is set to True.
    """
    if n_init <= 0:
        raise ValueError("Invalid number of initializations."
                         " n_init=%d must be bigger than zero." % n_init)
    random_state = check_random_state(random_state)

    if max_iter <= 0:
        raise ValueError('Number of iterations should be a positive number,'
                         ' got %d instead' % max_iter)

    best_inertia = np.infty
    X = as_float_array(X, copy=copy_x)
    tol = _tolerance(X, tol)

    # If the distances are precomputed every job will create a matrix of shape
    # (n_clusters, n_samples). To stop KMeans from eating up memory we only
    # activate this if the created matrix is guaranteed to be under 100MB. 12
    # million entries consume a little under 100MB if they are of type double.
    if precompute_distances == 'auto':
        n_samples = X.shape[0]
        precompute_distances = (n_clusters * n_samples) < 12e6
    elif isinstance(precompute_distances, bool):
        pass
    else:
        raise ValueError("precompute_distances should be 'auto' or True/False"
                         ", but a value of %r was passed" %
                         precompute_distances)

    # subtract of mean of x for more accurate distance computations
    if not sp.issparse(X) or hasattr(init, '__array__'):
        X_mean = X.mean(axis=0)
    if not sp.issparse(X):
        # The copy was already done above
        X -= X_mean

    if hasattr(init, '__array__'):
        init = check_array(init, dtype=np.float64, copy=True)
        _validate_center_shape(X, n_clusters, init)

        init -= X_mean
        if n_init != 1:
            warnings.warn(
                'Explicit initial center position passed: '
                'performing only one init in k-means instead of n_init=%d'
                % n_init, RuntimeWarning, stacklevel=2)
            n_init = 1

    # precompute squared norms of data points
    x_squared_norms = row_norms(X, squared=True)

    best_labels, best_inertia, best_centers = None, None, None
    if n_jobs == 1:
        # For a single thread, less memory is needed if we just store one set
        # of the best results (as opposed to one set per run per thread).
        for it in range(n_init):
            # run a k-means once
            labels, inertia, centers, n_iter_ = _kmeans_single(
                X, n_clusters, max_iter=max_iter, init=init, verbose=verbose,
                precompute_distances=precompute_distances, tol=tol,
                x_squared_norms=x_squared_norms, random_state=random_state)
            # determine if these results are the best so far
            if best_inertia is None or inertia < best_inertia:
                best_labels = labels.copy()
                best_centers = centers.copy()
                best_inertia = inertia
                best_n_iter = n_iter_
    else:
        # parallelisation of k-means runs
        seeds = random_state.randint(np.iinfo(np.int32).max, size=n_init)
        results = Parallel(n_jobs=n_jobs, verbose=0)(
            delayed(_kmeans_single)(X, n_clusters, max_iter=max_iter,
                                    init=init, verbose=verbose, tol=tol,
                                    precompute_distances=precompute_distances,
                                    x_squared_norms=x_squared_norms,
                                    # Change seed to ensure variety
                                    random_state=seed)
            for seed in seeds)
        # Get results with the lowest inertia
        labels, inertia, centers, n_iters = zip(*results)
        best = np.argmin(inertia)
        best_labels = labels[best]
        best_inertia = inertia[best]
        best_centers = centers[best]
        best_n_iter = n_iters[best]

    if not sp.issparse(X):
        if not copy_x:
            X += X_mean
        best_centers += X_mean

    if return_n_iter:
        return best_centers, best_labels, best_inertia, best_n_iter
    else:
        return best_centers, best_labels, best_inertia


def _kmeans_single(X, n_clusters, x_squared_norms, max_iter=300,
                   init='k-means++', verbose=False, random_state=None, tol=1e-3, precompute_distances=True):
    """A single run of k-means, assumes preparation completed prior.
    Parameters
    ----------
    X: array-like of floats, shape (n_samples, n_features)
        The observations to cluster.
    n_clusters: int
        The number of clusters to form as well as the number of
        centroids to generate.
    max_iter: int, optional, default 300
        Maximum number of iterations of the k-means algorithm to run.
    init: {'k-means++', 'random', or ndarray, or a callable}, optional
        Method for initialization, default to 'k-means++':
        'k-means++' : selects initial cluster centers for k-mean
        clustering in a smart way to speed up convergence. See section
        Notes in k_init for more details.
        'random': generate k centroids from a Gaussian with mean and
        variance estimated from the data.
        If an ndarray is passed, it should be of shape (k, p) and gives
        the initial centers.
        If a callable is passed, it should take arguments X, k and
        and a random state and return an initialization.
    tol: float, optional
        The relative increment in the results before declaring convergence.
    verbose: boolean, optional
        Verbosity mode
    x_squared_norms: array
        Precomputed x_squared_norms.
    precompute_distances : boolean, default: True
        Precompute distances (faster but takes more memory).
    random_state: integer or numpy.RandomState, optional
        The generator used to initialize the centers. If an integer is
        given, it fixes the seed. Defaults to the global numpy random
        number generator.
    Returns
    -------
    centroid: float ndarray with shape (k, n_features)
        Centroids found at the last iteration of k-means.
    label: integer ndarray with shape (n_samples,)
        label[i] is the code or index of the centroid the
        i'th observation is closest to.
    inertia: float
        The final value of the inertia criterion (sum of squared distances to
        the closest centroid for all observations in the training set).
    n_iter : int
        Number of iterations run.
    """
    random_state = check_random_state(random_state)

    best_labels, best_inertia, best_centers = None, None, None
    # init
    centers = k_means_._init_centroids(X, n_clusters, init, random_state=random_state,
                                       x_squared_norms=x_squared_norms)
    if verbose:
        print("Initialization complete")

    # Allocate memory to store the distances for each sample to its
    # closer center for reallocation in case of ties
    distances = np.zeros(shape=(X.shape[0],), dtype=np.float64)
    sample_weight = np.ones(shape=(X.shape[0],))

    # iterations
    for i in range(max_iter):
        centers_old = centers.copy()
        # labels assignment is also called the E-step of EM
        labels, inertia = \
            _labels_inertia(X, x_squared_norms, centers,
                            precompute_distances=precompute_distances,
                            distances=distances)

        # computation of the means is also called the M-step of EM
        if sp.issparse(X):
            centers = _k_means._centers_sparse(X, labels, n_clusters,
                                               distances)
        else:
            centers = _k_means._centers_dense(X, sample_weight, labels, n_clusters, distances)

        if verbose:
            print("Iteration %2d, inertia %.3f" % (i, inertia))

        if best_inertia is None or inertia < best_inertia:
            best_labels = labels.copy()
            best_centers = centers.copy()
            best_inertia = inertia

        shift = squared_norm(centers_old - centers)
        if shift <= tol:
            if verbose:
                print("Converged at iteration %d" % i)

            break

    if shift > 0:
        # rerun E-step in case of non-convergence so that predicted labels
        # match cluster centers
        best_labels, best_inertia = \
            _labels_inertia(X, x_squared_norms, best_centers,
                            precompute_distances=precompute_distances,
                            distances=distances)

    return best_labels, best_inertia, best_centers, i + 1


def _validate_center_shape(X, n_centers, centers):
    """Check if centers is compatible with X and n_centers"""
    if len(centers) != n_centers:
        raise ValueError('The shape of the initial centers (%s) '
                         'does not match the number of clusters %i'
                         % (centers.shape, n_centers))
    if centers.shape[1] != X.shape[1]:
        raise ValueError(
            "The number of features of the initial centers %s "
            "does not match the number of features of the data %s."
            % (centers.shape[1], X.shape[1]))


def _tolerance(X, tol):
    """Return a tolerance which is independent of the dataset"""
    if sp.issparse(X):
        variances = mean_variance_axis(X, axis=0)[1]
    else:
        variances = np.var(X, axis=0)
    return np.mean(variances) * tol


def _labels_inertia(X, x_squared_norms, centers,
                    precompute_distances=True, distances=None):
    """E step of the K-means EM algorithm.
    Compute the labels and the inertia of the given samples and centers.
    This will compute the distances in-place.
    Parameters
    ----------
    X: float64 array-like or CSR sparse matrix, shape (n_samples, n_features)
        The input samples to assign to the labels.
    x_squared_norms: array, shape (n_samples,)
        Precomputed squared euclidean norm of each data point, to speed up
        computations.
    centers: float64 array, shape (k, n_features)
        The cluster centers.
    precompute_distances : boolean, default: True
        Precompute distances (faster but takes more memory).
    distances: float64 array, shape (n_samples,)
        Pre-allocated array to be filled in with each sample's distance
        to the closest center.
    Returns
    -------
    labels: int array of shape(n)
        The resulting assignment
    inertia : float
        Sum of distances of samples to their closest cluster center.
    """
    n_samples = X.shape[0]
    # set the default value of centers to -1 to be able to detect any anomaly
    # easily
    labels = -np.ones(n_samples, np.int32)
    if distances is None:
        distances = np.zeros(shape=(0,), dtype=np.float64)
    # distances will be changed in-place
    if sp.issparse(X):
        inertia = k_means_._k_means._assign_labels_csr(
            X, x_squared_norms, centers, labels, distances=distances)
    else:
        if precompute_distances:
            return _labels_inertia_precompute_dense(X, x_squared_norms,
                                                    centers, distances)
        inertia = k_means_._k_means._assign_labels_array(
            X, x_squared_norms, centers, labels, distances=distances)
    return labels, inertia



def _labels_inertia_precompute_dense(X, x_squared_norms, centers, distances):
    """Compute labels and inertia using a full distance matrix.
    This will overwrite the 'distances' array in-place.
    Parameters
    ----------
    X : numpy array, shape (n_sample, n_features)
        Input data.
    x_squared_norms : numpy array, shape (n_samples,)
        Precomputed squared norms of X.
    centers : numpy array, shape (n_clusters, n_features)
        Cluster centers which data is assigned to.
    distances : numpy array, shape (n_samples,)
        Pre-allocated array in which distances are stored.
    Returns
    -------
    labels : numpy array, dtype=np.int, shape (n_samples,)
        Indices of clusters that samples are assigned to.
    inertia : float
        Sum of distances of samples to their closest cluster center.
    """
    n_samples = X.shape[0]
    k = centers.shape[0]
    all_distances = euclidean_distances(centers, X, x_squared_norms,
                                        squared=True)
    labels = np.empty(n_samples, dtype=np.int32)
    labels.fill(-1)
    mindist = np.empty(n_samples)
    mindist.fill(np.infty)


    n_samples = X.shape[0]
    k = centers.shape[0]
    max_cluster_size = get_clusters_size(n_samples, k)

    labels, mindist = initial_assignment(labels, mindist, n_samples, all_distances, max_cluster_size)
    all_points = np.arange(n_samples)

    for point in all_points:
        for point_dist in get_best_point_distances(point, all_distances):
            cluster_id, point_dist = point_dist
            # initial assignment
            if not is_cluster_full(cluster_id, max_cluster_size, labels):
                labels[point] = cluster_id
                mindist[point] = point_dist
                break

    # refinement of clustering
    transfer_list = []
    best_mindist = mindist.copy()
    best_labels = labels.copy()
    # sort all of the points from largest distance to smallest
    points_by_high_distance = np.argsort(mindist)[::-1]
    for point in points_by_high_distance:
        point_cluster = labels[point]

        # see if there is an opening on the best cluster for this point
        cluster_id, point_dist = get_best_cluster_for_point(point, all_distances)
        if not is_cluster_full(cluster_id, max_cluster_size, labels) and point_cluster != cluster_id:
            labels[point] = cluster_id
            mindist[point] = point_dist
            best_labels = labels.copy()
            best_mindist = mindist.copy()
            continue # on to the next point

        for swap_candidate in transfer_list:
            cand_cluster = labels[swap_candidate]
            if point_cluster != cand_cluster:

                # get the current dist of swap candidate
                cand_distance = mindist[swap_candidate]

                # get the potential dist of point
                point_distance = all_distances[cand_cluster, point]

                # compare
                if point_distance < cand_distance:

                    labels[point] = cand_cluster
                    mindist[point] = all_distances[cand_cluster, point]

                    labels[swap_candidate] = point_cluster
                    mindist[swap_candidate] = all_distances[point_cluster, swap_candidate]

                    if np.absolute(mindist).sum() <  np.absolute(best_mindist).sum():
                        # update the labels since the transfer was a success
                        best_labels = labels.copy()
                        best_mindist = mindist.copy()
                        break

                    else:
                        # reset since the transfer was not a success
                        labels = best_labels.copy()
                        mindist = best_mindist.copy()

        transfer_list.append(point)

    if n_samples == distances.shape[0]:
        # distances will be changed in-place
        distances[:] = mindist
    inertia = best_mindist.sum()

    return best_labels, inertia

def get_best_cluster_for_point(point, all_distances):
    """Gets the best cluster by distance for a point

    Argument
    --------
    point : int
        the point index

    Returns
    --------
    tuple
        (cluster_id, distance_from_cluster_center)
    """

    sorted_distances = get_best_point_distances(point, all_distances)
    cluster_id, point_dist = sorted_distances[0]
    return cluster_id, point_dist


def get_best_point_distances(point, all_distances):
    """Gets a sorted by best distance of clusters

    Argument
    --------
    point : int
        the point index

    Returns
    --------
    list of tuples sorted by point_dist
        example: [(cluster_id, point_dist), (cluster_id, point_dist)]
    """
    points_distances = all_distances[:, point]
    sorted_points = sort_adjust_row(points_distances)
    return sorted_points

def sort_adjust_row(points_distances):
    "Sorts the points row from smallest distance to lowest distance"
    return sorted([(cluster_id, point_dist) for cluster_id, point_dist in enumerate(points_distances)], key=lambda x: x[1])

def is_cluster_full(cluster_id, max_cluster_size, labels):
    """Determies in a cluster is full"""
    cluster_count = len(np.where(labels==cluster_id)[0])
    is_full = cluster_count >= max_cluster_size
    return is_full

def get_clusters_size(n_samples, n_clusters):
    """Gets the number of members per cluster for equal groups kmeans"""
    return (n_samples + n_clusters - 1) // n_clusters

def initial_assignment(labels, mindist, n_samples, all_distances, max_cluster_size):
    """Initial assignment of labels and mindist"""
    all_points = np.arange(n_samples)
    for point in all_points:
        for point_dist in get_best_point_distances(point, all_distances):
            cluster_id, point_dist = point_dist
            # initial assignment
            if not is_cluster_full(cluster_id, max_cluster_size, labels):
                labels[point] = cluster_id
                mindist[point] = point_dist
                break
    return labels, mindist



if __name__ == '__main__':
    ndist = 243 if WINNER_TAKE_ALL else 81

    # Extract Voters Positions
    voters = np.array(extractVoters("../../maps/g8/twoParties.map"))
    np.random.shuffle(voters)
    voters = voters[:NUM_VOTERS].tolist()
    V = np.vstack([np.array((i.x, i.y)) for i in voters])

    if LOAD_CLUSTERS:
        print('Loading Clusters')
        centroids = json.load(open('EqualGroupKmeans_centroids.json', 'r'))
    else:
        if BASIC_KMEANS:
            kmeans = MiniBatchKMeans(
                n_clusters=ndist, random_state=0, batch_size=32, max_iter=20, init_size=3 * 81).fit(V)
            centroids = BalancedClustering(ndist, kmeans.cluster_centers_, V)
        else:
            clf = EqualGroupsKMeans(n_clusters=ndist)
            clf.fit(V)
            np.save(open('EqualGroupKmeans.npy', 'wb'), clf)
            json.dump(clf.cluster_centers_.tolist(), open('EqualGroupKmeans_centroids.json', 'w'))
            centroids = clf.cluster_centers_

    # Generate Voronoi with generator points = cluster centroids
    # Note : Some generator points outside triangular boundary due to the error in coordinates.txt data
    districts = draw_districts(centroids)

    # Ensure valid
    centroids, districts, _ = sample_new_district_centers(centroids, districts, voters, sample=False)
    json.dump(centroids.tolist(), open('adjusted_data/centroids.npy', 'w'))
    np.save(open('adjusted_data/districts.npy', 'wb'), districts)

    # LOAD INITIAL DISTRICTS
    initial_districts = copy.deepcopy(districts)  # Keep a hardcopy of this

    best_score = -1
    for mut_idx in range(1000):
        # Randomly jiggle map N times
        all_candidate_districts = []
        all_candidate_centroids = []
        gerrymander_scores = []
        N = 10
        for n in range(N):
            candidate_centroids, candidate_districts, voters_by_district = sample_new_district_centers(
                centroids, districts, voters)
            all_candidate_districts.append(candidate_districts)
            all_candidate_centroids.append(candidate_centroids)
            gerrymander_score = asymmetry_score(candidate_districts, voters, voters_by_district)
            gerrymander_scores.append(gerrymander_score)

        gerrymander_scores = np.array(gerrymander_scores)
        best_idx = np.argsort(gerrymander_scores)[-1]
        centroids = all_candidate_centroids[best_idx]
        best_score = max(best_score, gerrymander_scores[best_idx])
        # Choose best district from gerrymandering perspective
        districts = all_candidate_districts[best_idx]

        print('Best score at {} is {}'.format(mut_idx, best_score))
        np.save(open('best_districts_at_{}'.format(mut_idx), 'w'), districts)
        json.dump(centroids.tolist(), open('best_centroids_at_{}'.format(mut_idx), 'w'))

    print('Best gerrymander score (-1, 1) is {}'.format(len(districts)))
