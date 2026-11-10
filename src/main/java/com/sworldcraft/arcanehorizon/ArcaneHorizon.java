package com.sworldcraft.arcanehorizon;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ArcaneHorizon.ID)
public final class ArcaneHorizon {
    public static final String ID = "arcanehorizon";
    public static final Logger LOG = LogUtils.getLogger();
    public ArcaneHorizon() {
        LOG.info("Arcane Horizon 0.5.0-alpha: Distant Horizons engine integration");
    }
}
