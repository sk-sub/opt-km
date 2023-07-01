public class PQWrapper<T> {

    public PQWrapper(T data, double cost) {
        this.data = data;
        this.cost = cost;
    }
    double cost;
    T data;
}
