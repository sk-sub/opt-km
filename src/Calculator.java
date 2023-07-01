public interface Calculator<T1, T2> {
    public Double getLowerBound(T1 u, T2 v);

    public Double getCost(T1 u, T2 v);
}
