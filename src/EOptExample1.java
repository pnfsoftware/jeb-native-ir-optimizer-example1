import com.pnfsoftware.jeb.core.Version;
import com.pnfsoftware.jeb.core.units.code.asm.cfg.BasicBlock;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.EUtil;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEAssign;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEImm;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEMem;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEStatement;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.opt.AbstractEOptimizer;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.opt.DataChainsUpdatePolicy;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.opt.OptimizerType;

/**
 * A sample IR optimizer.
 * 
 * @author Nicolas Falliere
 *
 */
public class EOptExample1 extends AbstractEOptimizer {

    public EOptExample1() {
        super(DataChainsUpdatePolicy.UPDATE_IF_OPTIMIZED);
        getPluginInformation().setName("Sample IR Optimizer #1");
        getPluginInformation().setDescription("Remove IR-statements reduced to \"*(&junk + delta) = xxx\"");
        getPluginInformation().setVersion(Version.create(1, 0, 0));

        // Standard optimizers are normally run, as part of the IR optimization stages in the decompilation pipeline
        setType(OptimizerType.NORMAL);
        //        // note the minus sign, the optimizer will be run before 
        //        setPreferredExecutionStage(-NativeDecompilationStage.LIFTING_COMPLETED.getId());
        //        setPostProcessingActionFlags(PPA_OPTIMIZATION_PASS_FULL);
    }

    // replace all IR statements previously reduced to "[..] = xxx" to ENop
    @Override
    public int perform() {
        logger.info("IR-CFG before running custom optimizer \"%s\":\n%s", getName(), EUtil.formatIR(ectx));

        final long garbageStart = 0x415882;
        final long garbageEnd = garbageStart + 0x100;

        int cnt = 0;
        for(int iblk = 0; iblk < cfg.size(); iblk++) {
            BasicBlock<IEStatement> b = cfg.get(iblk);
            for(int i = 0; i < b.size(); i++) {
                IEStatement stm = b.get(i);
                if(!(stm instanceof IEAssign)) {
                    continue;
                }

                IEAssign asg = (IEAssign)stm;
                if(!(asg.getLeftOperand() instanceof IEMem)) {
                    continue;
                }

                IEMem target = (IEMem)asg.getLeftOperand();
                if(!(target.getReference() instanceof IEImm)) {
                    continue;
                }
                ;

                IEImm wraddr = (IEImm)target.getReference();
                if(!wraddr.canReadAsAddress()) {
                    continue;
                }

                long addr = wraddr.getValueAsAddress();
                if(addr < garbageStart || addr >= garbageEnd) {
                    continue;
                }

                b.set(i, ectx.createNop(stm));
                cnt++;
            }
        }
        return postPerform(cnt);
    }
}