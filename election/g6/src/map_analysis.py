import sys
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

def get_average_gap(voters, party):
    # Get the mean difference between voter preference to a party and the average party preference
    n_voters = len(voters)  # number of voters
    total_party = 0  # total preference to a party
    total_mean = 0  # total average preference
    for i in voters:
        total_party += voters[i][party+1]
        total_mean += (sum(voters[i][2:]) / len(voters[i][2:]))
    avg_party = total_party / n_voters  # mean preference to a party
    avg_mean = total_mean / n_voters   # mean average preference
    gap = avg_party - avg_mean  # preference gap
    return gap

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

def check_dist_sides(dists, max_sides = 9):
    # Check if each district contains at most 9 sides
    violations = []  # districts that violate this requirement
    for i in dists:
        if len(dists[i]) > max_sides:
            violations.append((i, len(dists[i])))
    return violations

def check_dist_sizes(dist_voters, n_voters = 333333, max_dev = 0.1):
    # Check if number of voters in each district does not deviate from mean by more than 10%
    violations = []  # districts that violate this requirement
    n_dists = len(dist_voters)
    min_voters = (n_voters / n_dists) * (1 - max_dev)
    max_voters = (n_voters / n_dists) * (1 + max_dev)
    for i in dist_voters:
        if len(dist_voters[i]) < min_voters:
            violations.append((i, len(dist_voters[i]), int(min_voters) + 1))
        elif len(dist_voters[i]) > max_voters:
            violations.append((i, len(dist_voters[i]), int(max_voters)))
    return violations

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
        print("Error: Unable to handle cases with more than 2 parties.")
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
        print("Error: Unable to handle cases with more than 2 parties.")
        return
    wasted = get_wasted_votes(dist_results, dist_seats)
    gap = (wasted[-1][1] - wasted[-1][0]) / sum(dist_results[-1])
    return gap

def print_result(d, file, with_total = False):
    # Write a dictionary containing results to a file
    f = open(file, "w")
    if with_total:
        f.write("Total ")
        for j in range(len(d[-1])):
            f.write(str(d[-1][j]) + " ")
        f.write("\n")
    for i in d:
        if i >= 0:
            f.write(str(i) + " ")
            for j in range(len(d[i])):
                f.write(str(d[i][j]) + " ")
            f.write("\n")
    f.close()

def print_seats(dist_seats, file):
    # Write the election results to a file
    total_seats = get_total_seats(dist_seats)
    f = open(file, "w")
    f.write("Total ")
    for j in range(len(total_seats)):
        f.write(str(total_seats[j]) + " ")
    f.write("\n")
    for i in dist_seats:
        if i >= 0:
            f.write(str(i) + " ")
            for j in range(len(dist_seats[i])):
                f.write(str(dist_seats[i][j]) + " ")
            f.write("\n")
    f.close()

def get_new_voters(dist_voters, delta, sd = 0.05, truncate = True):
    # Get new party preferences for each voter following certain changes
    # Step 1: Initialization
    n_parties = len(delta)  # number of parties
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
                if truncate:
                    this_new_preference[p] = max(min(dist_voters[d][v][p+2] + np.random.normal(delta[p], sd), 1), 0)
                else:
                    this_new_preference[p] = dist_voters[d][v][p+2] + np.random.normal(delta[p], sd)
            new_dist_voters[d][v] = tuple(this_new_voter + this_new_preference)
    return new_dist_voters

def get_partisanship_bias(dist_voters, gap, n_rep, truncate = True):
    # Get partisanship bias
    # Preliminary version: See what happens after modifying party preferences
    n_parties = len(dist_voters[0][0]) - 2  # number of parties
    gap = abs(gap)  # average gap in voter preferences between each party and the mean
    if n_parties > 2:
        print("Error: Currently unable to handle cases with more than 2 parties.")
        return
    deltas = [-15 * gap, -10 * gap, -8 * gap, -6 * gap, -5 * gap, -4 * gap, -3 * gap]
    deltas += [-2 * gap, -1.5 * gap, -1.2 * gap, -1.1 * gap, -1 * gap, -.9 * gap, -.8 * gap, -.5 * gap]
    deltas += [0, .5 * gap, .8 * gap, .9 * gap, gap, 1.1 * gap, 1.2 * gap, 1.5 * gap, 2 * gap]
    deltas += [3 * gap, 4 * gap, 5 * gap, 6 * gap, 8 * gap, 10 * gap, 15 * gap]
    if abs(15 * gap) < 0.5:
        deltas = [-0.5, -0.4, -0.3, -0.2, -0.1, -0.05] + deltas + [0.05, 0.1, 0.2, 0.3, 0.4, 0.5]
    new_results = {}
    for d in deltas:
        # Get new voting results and number of seats for each delta
        new_voters = get_new_voters(dist_voters, [d, -d], abs(d) / 2, truncate)
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

def analyze_map(input_file, output_loc, log_all = False):
    # Put everything together, automatically analyze a map
    # Step 1: Initialization
    if output_loc[-1] != "/":
        output_loc += "/"
    log_file = output_loc + "log.txt"
    f = open(log_file, "w")
    f.write("Map Analysis\n")
    f.write("\n")
    # Step 2: Load a map containing voters and districts
    voters, dists = read_data(input_file)
    if len(dists) == 0:
        f.write("This map contains no districts.")
        f.close()
        return
    gap = get_average_gap(voters, 1)
    # Step 3: Group voters by districts
    dist_voters = get_dist_voters(voters, dists)
    # Step 4: Check if the districts satisfy requirements
    vio_sides = check_dist_sides(dists)
    if len(vio_sides) > 0:
        f.write(str(len(vio_sides)) + " district(s) have too many sides:\n")
        for i in range(len(vio_sides)):
            f.write("District " + str(vio_sides[i][0]) + " contains " + str(vio_sides[i][1]) + " sides\n")
        f.write("\n")
    vio_sizes = check_dist_sizes(dist_voters)
    if len(vio_sizes) > 0:
        f.write(str(len(vio_sizes)) + " district(s) contain too many or too few voters:\n")
        for i in range(len(vio_sizes)):
            f.write("District " + str(vio_sizes[i][0]) + " contains " + str(vio_sizes[i][1]) + " voters\n")
        f.write("\n")
    # Step 5: Get the election results in districts
    dist_results = get_dist_results(dist_voters)
    for i in range(len(dist_results[-1])):
        f.write("Party " + str(i + 1) + " gets " + str(dist_results[-1][i]) + " votes\n")
    f.write("\n")
    if log_all:
        print_result(dist_results, output_loc + "results_by_district.txt", True)
    dist_seats = get_all_dist_seats(dist_results, 3)
    total_seats = get_total_seats(dist_seats)
    for i in range(len(total_seats)):
        f.write("Party " + str(i + 1) + " gets " + str(total_seats[i]) + " representatives\n")
    f.write("\n")
    if log_all:
        print_seats(dist_seats, output_loc + "seats_by_district.txt")
    # Step 6: Analyze wasted votes (only for the 2-party case)
    wasted = get_wasted_votes(dist_results, dist_seats)
    if wasted is not None and log_all:
        print_result(wasted, output_loc + "wasted_votes_by_district.txt", True)
    # Step 7: Get partisanship gap curve
    new_results = get_partisanship_bias(dist_voters, gap, 3)
    if new_results is not None:
        get_partisanship_curve(new_results, output_loc + "partisanship_gap_curve.png")
    f.close()

if __name__ == "__main__":
    if len(sys.argv) == 3:
        input_file, output_loc, log_all = sys.argv[1], sys.argv[2], "False"
    else:
        input_file, output_loc, log_all = sys.argv[1], sys.argv[2], sys.argv[3]
    log_all = eval(log_all)
    analyze_map(input_file, output_loc, log_all)