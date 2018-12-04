package at.stefl.commons.math.matrix;

public class MatrixUtil {

	public static Matrix2d rotate2d(double angle) {
		return new Matrix2d(Math.cos(angle), Math.sin(angle), -Math.sin(angle),
				Math.cos(angle));
	}

	private MatrixUtil() {
	}

}