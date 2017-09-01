package org.paim.examsio;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.ModalityTransform;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.display.SourceImage;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import org.paim.commons.Exam;
import org.paim.commons.ExamSlice;


/**
 * Loads an exam from any source
 */
public class ExamLoader {
    
    public static Exam load(String file) throws ExamLoaderException {
        return load(new File(file));
    }

    public static Exam load(File file) throws ExamLoaderException {
        if (file.isDirectory()) {
            return loadFromFolder(file);
        } else {
            return loadFromFile(file);
        }
    }
    
    private static Exam loadFromFile(File file) throws ExamLoaderException {
        Exam exam = new Exam();
        loadFileToExam(file, exam);
        return exam;
    }
    
    private static Exam loadFromFolder(File folder) throws ExamLoaderException {
        File[] files = folder.listFiles((File dir, String name) -> Pattern.matches(".*\\.dcm", name.toLowerCase()));
        if (files.length == 0) {
            throw new ExamLoaderException("Diretório não possui exames! Conteúdo: " + folder.toString());
        }
        Exam exam = new Exam();
        for (File file : files) {
            loadFileToExam(file, exam);
        }
        return exam;
    }

    private static void loadFileToExam(File file, Exam exam) throws ExamLoaderException {
        AttributeList attributeList = new AttributeList();
        ExamSlice examSlice;
        try {
            attributeList.read(file);
            SourceImage img = new SourceImage(attributeList);
            ModalityTransform mod = new ModalityTransform(attributeList);
            for (int i = 0; i < img.getNumberOfBufferedImages(); i++) {
                examSlice = new ExamSlice();
                examSlice.setBufferedImage(img.getBufferedImage(i));
                examSlice.setSourceFile(file);
                examSlice.setColumns(img.getWidth());
                examSlice.setRows(img.getHeight());
                examSlice.setPadded(img.isPadded());
                examSlice.setPadValue(img.getPadValue());
                examSlice.setRescaleSlope(mod.getRescaleSlope(i));
                examSlice.setRescaleIntercept(mod.getRescaleIntercept(i));
                String s = Attribute.getDelimitedStringValuesOrEmptyString(attributeList, TagFromName.SliceThickness);
                examSlice.setSliceThickness(Float.parseFloat(s));
                exam.addExamSlice(examSlice);
            }
            
        } catch (IOException | DicomException ex) {
            throw new ExamLoaderException(ex);
        }        
    }

}
