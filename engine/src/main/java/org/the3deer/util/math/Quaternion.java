package org.the3deer.util.math;

/**
 * A quaternion simply represents a 3D rotation. The maths behind it is quite
 * complex (literally; it involves complex numbers) so I wont go into it in too
 * much detail. The important things to note are that it represents a 3d
 * rotation, it's very easy to interpolate between two quaternion rotations
 * (which would not be easy to do correctly with Euler rotations or rotation
 * matrices), and you can convert to and from matrices fairly easily. So when we
 * need to interpolate between rotations we'll represent them as quaternions,
 * but when we need to apply the rotations to anything we'll convert back to a
 * matrix.
 * <p>
 * An quick introduction video to quaternions:
 * https://www.youtube.com/watch?v=SCbpxiCN0U0
 * <p>
 * and a slightly longer one:
 * https://www.youtube.com/watch?v=fKIss4EV6ME&t=0s
 *
 * @author Karl
 */
public class Quaternion {

    private float[] matrix;
    private float x, y, z, w;

    public Quaternion(float[] matrix){
        this.matrix = matrix;
    }

    public Quaternion() {
        this(0,0,0,1);
    }

    /**
     * Creates a quaternion and normalizes it.
     *
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }


    /**
     * Normalizes the quaternion.
     */
    public void normalize() {
        float mag = (float) Math.sqrt(w * w + x * x + y * y + z * z);
        w /= mag;
        x /= mag;
        y /= mag;
        z /= mag;
    }

    /**
     * Converts the quaternion to a 4x4 matrix representing the exact same
     * rotation as this quaternion. (The rotation is only contained in the
     * top-left 3x3 part, but a 4x4 matrix is returned here for convenience
     * seeing as it will be multiplied with other 4x4 matrices).
     * <p>
     * More detailed explanation here:
     * http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToMatrix/
     *
     * @return The rotation matrix which represents the exact same rotation as
     * this quaternion.
     */
    public float[] toRotationMatrix(float[] matrix) {
        if (this.matrix != null){
            System.arraycopy(this.matrix,0,matrix,0,matrix.length);
            return matrix;
        }

        final float xy = x * y;
        final float xz = x * z;
        final float xw = x * w;
        final float yz = y * z;
        final float yw = y * w;
        final float zw = z * w;
        final float xSquared = x * x;
        final float ySquared = y * y;
        final float zSquared = z * z;
        matrix[0] = 1 - 2 * (ySquared + zSquared);
        matrix[1] = 2 * (xy - zw);
        matrix[2] = 2 * (xz + yw);
        matrix[3] = 0;
        matrix[4] = 2 * (xy + zw);
        matrix[5] = 1 - 2 * (xSquared + zSquared);
        matrix[6] = 2 * (yz - xw);
        matrix[7] = 0;
        matrix[8] = 2 * (xz - yw);
        matrix[9] = 2 * (yz + xw);
        matrix[10] = 1 - 2 * (xSquared + ySquared);
        matrix[11] = 0;
        matrix[12] = 0;
        matrix[13] = 0;
        matrix[14] = 0;
        matrix[15] = 1;
        return matrix;
    }

    /**
     * Extracts the rotation part of a transformation matrix and converts it to
     * a quaternion using the magic of maths.
     * <p>
     * More detailed explanation here:
     * http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/index.htm
     *
     * @param matrix - the transformation matrix containing the rotation which this
     *               quaternion shall represent.
     */
    public static Quaternion fromMatrix(float[] matrix) {
        float w, x, y, z;
        float diagonal = matrix[0] + matrix[5] + matrix[10];
        if (diagonal > 0) {
            float w4 = (float) (Math.sqrt(diagonal + 1f) * 2f);
            w = w4 / 4f;
            x = (matrix[9] - matrix[6]) / w4;
            y = (matrix[2] - matrix[8]) / w4;
            z = (matrix[4] - matrix[1]) / w4;
        } else if ((matrix[0] > matrix[5]) && (matrix[0] > matrix[10])) {
            float x4 = (float) (Math.sqrt(1f + matrix[0] - matrix[5] - matrix[10]) * 2f);
            w = (matrix[9] - matrix[6]) / x4;
            x = x4 / 4f;
            y = (matrix[1] + matrix[4]) / x4;
            z = (matrix[2] + matrix[8]) / x4;
        } else if (matrix[5] > matrix[10]) {
            float y4 = (float) (Math.sqrt(1f + matrix[5] - matrix[0] - matrix[10]) * 2f);
            w = (matrix[2] - matrix[8]) / y4;
            x = (matrix[1] + matrix[4]) / y4;
            y = y4 / 4f;
            z = (matrix[6] + matrix[9]) / y4;
        } else {
            float z4 = (float) (Math.sqrt(1f + matrix[10] - matrix[0] - matrix[5]) * 2f);
            w = (matrix[4] - matrix[1]) / z4;
            x = (matrix[2] + matrix[8]) / z4;
            y = (matrix[6] + matrix[9]) / z4;
            z = z4 / 4f;
        }
        return new Quaternion(x, y, z, w);
    }

    /**
     * Interpolates between two quaternion rotations and returns the resulting
     * quaternion rotation. The interpolation method here is "nlerp", or
     * "normalized-lerp". Another mnethod that could be used is "slerp", and you
     * can see a comparison of the methods here:
     * https://keithmaggio.wordpress.com/2011/02/15/math-magician-lerp-slerp-and-nlerp/
     * <p>
     * and here:
     * http://number-none.com/product/Understanding%20Slerp,%20Then%20Not%20Using%20It/
     *
     * @param result resulting interpolation
     * @param a
     * @param b
     * @param blend  - a value between 0 and 1 indicating how far to interpolate
     *               between the two quaternions.
     * @return The resulting interpolated rotation in quaternion format.
     */
    public static void interpolate(Quaternion result, Quaternion a, Quaternion b, float blend) {
        float dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z;
        float blendI = 1f - blend;
        if (dot < 0) {
            result.w = blendI * a.w + blend * -b.w;
            result.x = blendI * a.x + blend * -b.x;
            result.y = blendI * a.y + blend * -b.y;
            result.z = blendI * a.z + blend * -b.z;
        } else {
            result.w = blendI * a.w + blend * b.w;
            result.x = blendI * a.x + blend * b.x;
            result.y = blendI * a.y + blend * b.y;
            result.z = blendI * a.z + blend * b.z;
        }
        result.normalize();
    }

    /*public static void interpolate(Quaternion a, Quaternion b, float blend, float[] output) {
        Quaternion result = new Quaternion(0, 0, 0, 1);
        float dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z;
        float blendI = 1f - blend;
        if (dot < 0) {
            result.w = blendI * a.w + blend * -b.w;
            result.x = blendI * a.x + blend * -b.x;
            result.y = blendI * a.y + blend * -b.y;
            result.z = blendI * a.z + blend * -b.z;
        } else {
            result.w = blendI * a.w + blend * b.w;
            result.x = blendI * a.x + blend * b.x;
            result.y = blendI * a.y + blend * b.y;
            result.z = blendI * a.z + blend * b.z;
        }
        result.normalize();
        result.toRotationMatrix(output);
    }*/

    @Override
    public String toString() {
        return "Quaternion{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }

    public float[] toEuler() {

        Quaternion q = this;
        // roll (x-axis rotation)
        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        float roll = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = 2 * (q.w * q.y - q.z * q.x);
        final float pitch;
        if (Math.abs(sinp) >= 1)
            pitch = (float) Math.copySign (Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            pitch = (float) Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        float yaw = (float) Math.atan2(siny_cosp, cosy_cosp);

        return  new float[]{roll, pitch, yaw, 1};
    }

    /**
     * Returns the conjugate quaternion of the instance.
     *
     * @return the conjugate quaternion
     */
    public Quaternion getConjugate() {
        return new Quaternion(-this.x, -this.y, -this.z, this.w);
    }

    /**
     * Returns the Hamilton product of two quaternions.
     *
     * @param q1 First quaternion.
     * @param q2 Second quaternion.
     * @return the product {@code q1} and {@code q2}, in that order.
     */
    public static Quaternion multiply(final Quaternion q1, final Quaternion q2) {
        // Components of the first quaternion.
        final double q1a = q1.getW();
        final double q1b = q1.getX();
        final double q1c = q1.getY();
        final double q1d = q1.getZ();

        // Components of the second quaternion.
        final double q2a = q2.getW();
        final double q2b = q2.getX();
        final double q2c = q2.getY();
        final double q2d = q2.getZ();

        // Components of the product.
        final double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
        final double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
        final double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
        final double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;

        return new Quaternion((float)x, (float)y, (float)z, (float)w);
    }

    public static Quaternion getQuaternion(float[] axis, float angle) {
        float w = (float) Math.cos(angle / 2f);
        float x = (float) (axis[0] * Math.sin(angle / 2f));
        float y = (float) (axis[1] * Math.sin(angle / 2f));
        float z = (float) (axis[2] * Math.sin(angle / 2f));
        return new Quaternion(x,y,z,w);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getW() {
        return w;
    }

    public float[] toAxisAngle(){
        if (w > 1) normalize(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised

        double angle = 2 * Math.acos(w);

        float[] ret = new float[]{x,y,z, (float) Math.toDegrees(angle)};

        double s = Math.sqrt(1-w*w); // assuming quaternion normalised then w is less than 1, so term always positive.
        if (s >= 0.001) { // test to avoid divide by zero, s is always positive due to sqrt
            Math3DUtils.normalize(ret);
        } else {
            // if s close to zero then direction of axis not important
            //x = q1.x; // if it is important that axis is normalised then replace with x=1; y=z=0;
            //y = q1.y;
            //z = q1.z;
        }
        return ret;
    }

    public float getAngle(){
        return (float) (2*Math.acos(w));
    }

    public Quaternion getInverse() {
        return new Quaternion(-this.x, -this.y, -this.z, this.w);
    }

}
