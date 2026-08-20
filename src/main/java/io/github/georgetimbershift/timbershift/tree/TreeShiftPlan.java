package io.github.georgetimbershift.timbershift.tree;

import java.util.List;

public record TreeShiftPlan(ShiftPlanStatus status, List<LogMove> moves) {
    public TreeShiftPlan {
        moves = List.copyOf(moves);
    }

    public boolean ready() {
        return status == ShiftPlanStatus.READY && !moves.isEmpty();
    }

    public static TreeShiftPlan empty(ShiftPlanStatus status) {
        return new TreeShiftPlan(status, List.of());
    }
}
