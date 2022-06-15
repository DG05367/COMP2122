package pt.up.fe.comp; 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import worker.Generator;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.OllirErrorException;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

public class Backend implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult result) {
        ClassUnit unit = result.getOllirClass();

        try {
            unit.checkMethodLabels();
            unit.buildCFGs(); 
            unit.outputCFGs(); 
            unit.buildVarTables();
            unit.show();

            String code = new Generator(unit).generateClass();
            List<Report> reports = new ArrayList<>();

            return new JasminResult(result, code, reports);
        }
        catch(OllirErrorException e) {
            return new JasminResult(unit.getClassName(), null, Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }
    }
    
}
