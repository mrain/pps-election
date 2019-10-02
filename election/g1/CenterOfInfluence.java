package election.g1;

import java.awt.geom.*;

public class CenterOfInfluence {

	private Point2D location;
	private List<Double> preferences;

	public CenterOfInfluence(Point2D location, List<Double> preferences) {
		this.location = location;
		this.preferences = preferences;
	}

	public Point2D getLocation() {
		return location;
	}

	public List<Double> getPreferences() {
		return preferences;
	}

}