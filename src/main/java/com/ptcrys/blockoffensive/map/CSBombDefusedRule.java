package com.ptcrys.blockoffensive.map;

import com.phasetranscrystal.fpsmatch.core.map.BlastBombState;
import com.phasetranscrystal.fpsmatch.core.match.RoundLifecycle;
import com.phasetranscrystal.fpsmatch.core.match.RoundResult;
import com.phasetranscrystal.fpsmatch.core.match.RoundRuleWithContext;
import com.phasetranscrystal.fpsmatch.core.match.RoundContext;

import java.util.Optional;

class CSBombDefusedRule implements RoundRuleWithContext<String, CSRoundResultReason> {
    @Override
    public Optional<RoundResult<String, CSRoundResultReason>> evaluate(RoundLifecycle<String, CSRoundResultReason> lifecycle, RoundContext context) {
        CSRoundContext ctx = (CSRoundContext) context;
        if (ctx.blastState() == BlastBombState.DEFUSED) {
            return Optional.of(new RoundResult<>(ctx.ctTeamName(), CSRoundResultReason.DEFUSE_BOMB));
        }
        return Optional.empty();
    }
}
