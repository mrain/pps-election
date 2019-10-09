import numpy as np
import matplotlib.pyplot as plt
from shapely.geometry import Point, Polygon

def read_data(file):
    # Read the data for voters and districts
    # Step 1: Open the file
    with open(file) as f:
        contents = f.readlines()
    contents = [x.strip() for x in contents]
    # Step 2: Get the voters
    n_voters = int(contents[0].split()[0])
    voters = {}
    for i in range(n_voters):
        this_voter_str = contents[i+1].split()
        this_voter = tuple([eval(x) for x in this_voter_str])
        voters[i] = this_voter
    # Step 3: Get the districts
    n_dists = int(contents[n_voters+1])
    dists = {}
    for i in range(n_dists):
        dists[i] = []
        this_dist_str = contents[i+n_voters+2].split()
        n_vertices = int(this_dist_str[0])
        for j in range(n_vertices):
            x, y = eval(this_dist_str[j*2+1]), eval(this_dist_str[j*2+2])
            dists[i].append((x, y))
    return (voters, dists)

def get_dist_voters(voters, dists):
    # Get the voters in each district (Warning: May be slow)
    # Step 1: Initialization
    dist_voters = {}
    for d in dists:
        dist_voters[d] = []
    # Step 2: For each voter determine which district the voter belongs to
    for v in voters:
        this_voter = voters[v]
        this_x, this_y = this_voter[0], this_voter[1]  # location of the voter
        for d in dists:
            this_dist = dists[d]
            if Polygon(this_dist).contains(Point(this_x, this_y)):
                # The voter belongs to this district, add it
                dist_voters[d].append(this_voter)
                break
    return dist_voters

def get_dist_results(dist_voters):
    # Get the voting results in each district (number of votes each party gets)
    # Step 1: Initialization
    n_parties = len(dist_voters[0][0]) - 2  # number of parties
    dist_results = {}
    dist_results[-1] = [0 for i in range(n_parties)]  # overall results
    for d in dist_voters:
        dist_results[d] = [0 for i in range(n_parties)]  # results in each district
    # Step 2: Calculate results district by district
    for d in dist_voters:
        for v in range(len(dist_voters[d])):
            this_pref = dist_voters[d][v][2:]  # party preferences for this voter
            this_party = np.argmax(this_pref)  # the party most preferred by this voter
            dist_results[-1][this_party] += 1
            dist_results[d][this_party] += 1
    return dist_results

def get_one_dist_seats(dist_votes, n_rep):
    # Get the number of seats for each party in one district
    # Step 1: Get the percentages of votes for each party
    n_parties = len(dist_votes)  # number of parties
    votes = [dist_votes[i] / sum(dist_votes) for i in range(n_parties)]
    # Step 2: Calculate the number of seats each party should get
    seats = [0 for i in range(n_parties)]
    n_elected = 0
    # Assume there are three representatives per district
    # First round: Award one seat to each party with at least 25% of votes
    for i in range(n_parties):
        if n_elected < n_rep and votes[i] >= 1 / (n_rep + 1):
            seats[i] += 1
            n_elected += 1
            votes[i] -= 1 / (n_rep + 1)
    # Second round: To each party award one seat every 25% of votes
    # At most one party may be awarded seats in this round (three-seat case)
    for i in range(n_parties):
        while n_elected < n_rep and votes[i] >= 1 / (n_rep + 1):
            seats[i] += 1
            n_elected += 1
            votes[i] -= 1 / (n_rep + 1)
    # Third round: Award remaining seats based on ranking of remaining votes
    while n_elected < n_rep:
        p = np.argmax(votes)
        seats[p] += 1
        n_elected += 1
        votes[p] = 0
    return seats
    
def get_all_dist_seats(dist_results, n_rep):
    # Get the numbers of seats for each party in all districts
    # Step 1: Initialization
    dist_seats = {}
    # Step 2: Calculate numbers of seats district by district
    for d in dist_results:
        dist_seats[d] = get_one_dist_seats(dist_results[d], n_rep)
    return dist_seats

def get_total_seats(dist_seats):
    # Get the total number of seats for each party
    # Step 1: Initialization
    n_parties = len(dist_seats[-1])  # number of parties
    total_seats = [0 for i in range(n_parties)]
    # Step 2: Calculate the total number of seats for each party
    for d in dist_seats:
        # d = -1 for the "overall" district, not needed here
        if d != -1:
            for i in range(n_parties):
                total_seats[i] += dist_seats[d][i]
    return total_seats

def get_wasted_votes(dist_results, dist_seats):
    # Get the number of wasted votes in each district for each party
    n_parties = len(dist_results[-1])  # number of parties
    n_seats = sum(dist_seats[-1])  # number of seats per district
    if n_parties > 2:
        print("Error: Currently unable to handle cases with more than 2 parties.")
        return
    # Step 1: Calculate how many votes each party needs at least to get the current number of seats
    needed = {}
    for d in dist_results:
        if d != -1:
            needed[d] = [0 for i in range(n_parties)]
            for i in range(n_parties):
                if dist_seats[d][i] == 0:
                    # Party gets no seat, all seats are wasted
                    needed[d][i] = 0
                else:
                    # Party gets some seats, needs at least the following number of votes
                    needed[d][i] = int(sum(dist_results[d]) * dist_seats[d][i] / (1 + n_seats)) + 1
    # Step 2: Calculate how many votes are wasted for each party
    wasted = {}
    wasted[-1] = [0 for i in range(n_parties)]  # total wasted votes
    wasted[-2] = [0 for i in range(n_parties)]  # total wasted votes (normalized by district sizes)
    for d in needed:
        wasted[d] = [dist_results[d][i] - needed[d][i] for i in range(n_parties)]
    for d in needed:
        for i in range(n_parties):
            wasted[-1][i] += wasted[d][i]
            wasted[-2][i] += wasted[d][i] / sum(dist_results[d])
    return wasted

def get_efficiency_gap(dist_results, dist_seats):
    # Get the efficiency gap
    # Result is positive if Party 2 wasts more votes (Party 1 has advantage), negative otherwise
    n_parties = len(dist_results[-1])  # number of parties
    if n_parties > 2:
        print("Error: Currently unable to handle cases with more than 2 parties.")
        return
    wasted = get_wasted_votes(dist_results, dist_seats)
    gap = (wasted[-1][1] - wasted[-1][0]) / sum(dist_results[-1])
    return gap

def get_new_voters(dist_voters, delta, sd = 0.05, seed = 1234):
    # Get new party preferences for each voter following certain changes
    # Step 1: Initialization
    n_parties = len(delta)  # number of parties
    np.random.seed(seed)
    new_dist_voters = {}
    for d in dist_voters:
        new_dist_voters[d] = dist_voters[d][:]
    # Step 2: Change party preferences
    for d in new_dist_voters:
        for v in range(len(new_dist_voters[d])):
            this_new_voter = [dist_voters[d][v][0], dist_voters[d][v][1]]  # location of the voter
            this_new_preference = [0 for i in range(n_parties)]
            for p in range(n_parties):
                # Change the party preferences with delta and random noise
                this_new_preference[p] = max(min(dist_voters[d][v][p+2] + np.random.normal(delta[p], sd), 1), 0)
            new_dist_voters[d][v] = tuple(this_new_voter + this_new_preference)
    return new_dist_voters

def get_partisanship_bias(dist_voters, n_rep):
    # Get partisanship bias
    # Preliminary version: See what happens after modifying party preferences
    n_parties = len(dist_voters[0][0]) - 2  # number of parties
    if n_parties > 2:
        print("Error: Currently unable to handle cases with more than 2 parties.")
        return
    deltas = [-0.32, -0.16, -0.08, -0.04, -0.02, 0, 0.02, 0.04, 0.08, 0.16, 0.32]
    new_results = {}
    for d in deltas:
        # Get new voting results and number of seats for each delta
        new_voters = get_new_voters(dist_voters, [d, -d], abs(d) / 2)
        new_dist_results = get_dist_results(new_voters)
        new_dist_seats = get_all_dist_seats(new_dist_results, n_rep)
        new_total_seats = get_total_seats(new_dist_seats)
        new_results[d] = {"results": new_dist_results[-1], "seats": new_total_seats}
    return new_results

def get_partisanship_curve(new_results, file):
    # Get partisanship curve
    pvs = []
    pss = []
    for d in new_results:
        pv = new_results[d]["results"][:][0] / sum(new_results[d]["results"])
        ps = new_results[d]["seats"][:][0] / sum(new_results[d]["seats"])
        pvs.append(pv)
        pss.append(ps)
    plt.plot(pvs, pss, "b-")
    plt.title(file)
    plt.xlabel("Percentage of votes")
    plt.ylabel("Percentage of seats")
    plt.hlines(0.5, 0, 1, "k", "dashed")
    plt.vlines(0.5, 0, 1, "k", "dashed")
    plt.savefig(file)