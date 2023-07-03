import java.util.*;

public class OptKM<T1, T2> {
    Map<T1, Map<T2, Double>> costs = new HashMap<>();       // 存放已计算的真实成本
    Map<T1, Double> _l_u = new HashMap<>();
    Map<T2, Double> _l_v = new HashMap<>();
    Map<T1, Set<T2>> adj_u_v = new HashMap<>();     // 邻接表
    Map<T1, PriorityQueue<PQWrapper<T2>>> _Q_u = new HashMap<>();
    Set<T1> freeU;
    Set<T2> freeV;
    Map<T1, T2> M = new HashMap<>();
    Map<T2, T1> revM = new HashMap<>();         // 逆向的M
    Set<T1> S;
    Set<T2> N_S;
    Calculator<T1, T2> calculator;
    int m;

    Set<T1> U;
    Set<T2> V;
    int computed = 0;           // for debugging
    Map<T1, T2> augPath = new HashMap<>();

    public OptKM(Set<T1> U, Set<T2> V, Calculator<T1, T2> calculator) {
        this.U = U;
        this.V = V;
        freeU = new HashSet<>(U);
        freeV = new HashSet<>(V);
        this.calculator = calculator;
        m = U.size();            // TODO 暂时默认 |U| = |V|
    }

    /** Algorithm 1 */
    public Map<T1, T2> optKuhnMunkres() {

        // 初始化工作
        for (T1 u: U) {
            PriorityQueue<PQWrapper<T2>> pq = new PriorityQueue<>(Comparator.comparingDouble(o -> o.cost));

            Set<T2> adj = new HashSet<>();
            for (T2 v : V) {
                pq.add(new PQWrapper<T2>(v, calculator.getLowerBound(u, v)));
                adj.add(v);
            }
            adj_u_v.put(u, adj);
            _Q_u.put(u, pq);
        }

        S = new HashSet<>();
        N_S = new HashSet<>();


        while (M.size() < m) {
//            System.out.println("M size " + M.size());

            T1 u = null;
            T2 v = null;

            boolean found = false;

            Set<T1> unmarkedFreeU = new HashSet<>(freeU);       // TODO 这个 mark 什么时候重置
            while (unmarkedFreeU.size() > 0 && !found) {
                u = unmarkedFreeU.iterator().next();
                unmarkedFreeU.remove(u);
                found = findAugmentPath(u);
            }
            if (found) {            // 根据增广路更新 M

                for (T1 ru: augPath.keySet()) {
                    T2 rv = M.get(ru);
                    if (rv != null) {
                        removeMatch(ru, rv);
                    }
                }
                for (T1 nu: augPath.keySet()) {
                    S.add(nu);
                    N_S.add(augPath.get(nu));
                    addMatch(nu, augPath.get(nu));
                }
                System.out.printf("Found new path, total %d\n", M.size());

            } else {
                updateLabels();
            }
        }
        return M;
    }

    /** Algorithm 2 */
    private boolean findAugmentPath(T1 u) {
//        System.out.println("findAugmentPath" + " " + u);

        augPath = new HashMap<>();

        Set<T2> usedV = new HashSet<>();

        Queue<T1> PQ = new ArrayDeque<>();
        PQ.add(u);

        S = new HashSet<>();
        N_S = new HashSet<>();

        while (PQ.size() > 0) {
            T1 x = PQ.poll();
            S.add(x);
            for (T2 v : adj_u_v.get(x)) {
                if (c(x, v) == null && LB_r(x, v) <= 0) {
                    Set<PQWrapper<T2>> E = new HashSet<>();
                    while (LB_r(x, v) <= 0 && c(x, v) == null) {
                        PQWrapper<T2> ele = Q_u(x).poll();
                        T2 e = ele.data;
                        if (e == v) {
                            computeCost(x, v);
                            break;
                        } else {
                            E.add(ele);
                            // TODO 文章说这里要 Add 𝑒 to set 𝐸 and update 𝐿𝐵𝑟 (𝑥, 𝑣)
                        }
                    }
                    Q_u(u).addAll(E);        // Re-insert all 𝑒 ∈ 𝐸 back to 𝑄𝑥 by 𝐿𝐵(𝑥, 𝑒)
                }

                if (c(x, v) != null && c_r(x, v) == 0) {

                    N_S.add(v);

//                    System.out.println("Found one");
                    if (freeV.contains(v)) {
                        augPath.put(x, v);
                        return true;
                    } else {
                        T1 origU = revM.get(v);
                        augPath.put(x, v);
                        PQ.add(origU);                  // TODO 感觉这个地方造成了死循环，同一个 u 被反复地加入到 PQ 里
//                        System.out.printf("Add %s back to PQ\n", origU);
                    }
                }
            }
        }
        return false;
    }

    /** Algorithm 3 */
    private void updateLabels() {
        System.out.println("updateLabels");
        double _delta = delta();        // TODO delta 和 delta_cand 是什么关系
        System.out.println("delta=" + _delta);
        double delta_cand = _delta;
        for (T1 u : S) {
            Set<PQWrapper<T2>> E = new HashSet<>();
            while (LB_r(u) < delta_cand) {
                PQWrapper<T2> ele = Q_u(u).poll();
                T2 e = ele.data;
                if (!N_S.contains(e)) {
                    double _LB_r_u_e = ele.cost - l_u(u) - l_v(e);      // ele.cost 就是 LB(u, e)
                    if (_LB_r_u_e < delta_cand) {
                        computeCost(u, e);
                        if (c_r(u, e) < delta_cand) {
                            System.out.println("update delta");
                            delta_cand = c_r(u, e);
                        }
                    } else {
                        E.add(ele);
                    }
                } else {
                    E.add(ele);
                }
            }
            Q_u(u).addAll(E);
        }
        _delta = delta_cand;        // TODO delta 是在这里被更新吗
        for (T1 u : S) {
            _l_u.put(u, l_u(u) + _delta);
        }
        for (T2 v : N_S) {
            _l_v.put(v, l_v(v) - _delta);
        }
    }

    private double LB_r(T1 u, T2 v) {
        return LB_Q_u(u) - l_u(u) - l_v(v);
    }

    private double LB_r(T1 u) {
        double alpha = Double.MIN_VALUE;
        for (T2 v : V) {
            if (N_S.contains(v))
                continue;
            alpha = Math.max(alpha, l_v(v));
        }
        return LB_Q_u(u) - l_u(u) - alpha;
    }

    private double LB_Q_u(T1 u) {       // TODO 当 Q_u 为空时怎么办
        if (Q_u(u).size() == 0)
            return 0;
        return Q_u(u).peek().cost;
    }

    private double l_u(T1 u) {
        _l_u.putIfAbsent(u, 0.0);
        return _l_u.get(u);
    }

    private double l_v(T2 v) {
        _l_v.putIfAbsent(v, 0.0);
        return _l_v.get(v);
    }

    private Double c(T1 u, T2 v) {
        if (!costs.containsKey(u)) {
            costs.put(u, new HashMap<>());
        }
        return costs.get(u).get(v);
    }

    private double c_r(T1 u, T2 v) {
        return c(u, v) - l_u(u) - l_v(v);
    }

    private double delta() {
        double _delta = Double.MAX_VALUE;
        for (T1 u : S) {
            for (T2 v : V) {
                if (N_S.contains(v))
                    continue;
                if (c(u, v) == null)
                    continue;
                _delta = Math.min(_delta, c_r(u, v));
            }
        }
        return _delta;
    }

    private PriorityQueue<PQWrapper<T2>> Q_u(T1 u) {
        return _Q_u.get(u);
    }

    private double computeCost(T1 u, T2 v) {
        System.out.printf("Compute cost of %s to %s, total %d\n", u, v, ++computed);
        double res =  calculator.getCost(u, v);
        costs.putIfAbsent(u, new HashMap<>());
        costs.get(u).put(v, res);
        return res;
    }

    private void addMatch(T1 u, T2 v) {
        M.put(u, v);
        revM.put(v, u);
        freeU.remove(u);
        freeV.remove(v);
        System.out.printf("Match %s to %s, total %d\n", u, v, M.size());
    }

    private void removeMatch(T1 u, T2 v) {
        M.remove(u);
        revM.remove(v);
        freeU.add(u);
        freeV.add(v);
        System.out.printf("Remove match %s to %s, total %d\n", u, v, M.size());
    }

    private boolean existMatch(T1 u, T2 v) {
        return M.containsKey(u) && M.get(u) == v;
    }
}

