package ru.kck.usb.zags.fataloiv.transform;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xmlunit.validation.Languages;
import org.xmlunit.validation.ValidationProblem;
import org.xmlunit.validation.ValidationResult;
import org.xmlunit.validation.Validator;
import ru.usb.commons.utils.UsbXmlUtil;
import ru.usb.smev3.autogenerated.types.RejectCode;
import ru.usb.smev3.message.SmevMessageData;
import ru.usb.smev3.message.SmevOutcomeRejectParams;
import ru.usb.standard.message.income.StandardClientIncomeMessage;

import javax.xml.transform.stream.StreamSource;
import java.nio.charset.Charset;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ZagsFatalOivProducerTransformerTest Class
 *
 * @author Ivan V. Kulynych
 * @version 09/06/2016
 */

public class ZagsFatalOivStandart2SmevTransformationTest{
    ZagsFatalOivStandart2SmevTransformation transformation;

    @Before
    public void setUp(){
        transformation = new ZagsFatalOivStandart2SmevTransformation();
    }

    @Test
    public void testTransforms()throws Exception{
        testTransform("0");
    }

    public void testTransform(String numMes)throws Exception{
        StandardClientIncomeMessage standardClientIncomeMessage=EasyMock.createMock(StandardClientIncomeMessage.class);
        Element bodyElement=UsbXmlUtil.convertStreamToDocument(getClass().getClassLoader().getResourceAsStream("samples/tests/standartResponse"+numMes+".xml")).getDocumentElement();
        EasyMock.expect(standardClientIncomeMessage.getBody()).andReturn(bodyElement);
        EasyMock.expect(standardClientIncomeMessage.getAttachments()).andReturn(null).anyTimes();

        EasyMock.replay(standardClientIncomeMessage);
        SmevMessageData smevMessageData=transformation.transform(standardClientIncomeMessage);
        EasyMock.verify(standardClientIncomeMessage);

        Assert.assertNotNull(smevMessageData);
        String etalon=IOUtils.toString(getClass().getClassLoader().getResourceAsStream("samples/tests/smevResponse" + numMes + ".xml"),Charset.forName("UTF-8"));
        String result=UsbXmlUtil.convertDomTreeToString(smevMessageData.getBody(),true,false);
        System.out.println("===============================================================================================");
        System.out.println(result);

        Validator v=Validator.forLanguage(Languages.W3C_XML_SCHEMA_NS_URI);
        v.setSchemaSources(new StreamSource(getClass().getClassLoader().getResourceAsStream("xsd/commons/zags-fataloiv-types.xsd")),
                new StreamSource(getClass().getClassLoader().getResourceAsStream("xsd/zags-fataloiv-ru-root.xsd")));
        ValidationResult r=v.validateInstance(new StreamSource(IOUtils.toInputStream(result,Charset.forName("UTF-8"))));
        if(!r.isValid()){
            for(ValidationProblem problem:r.getProblems()){
                System.out.println(problem.getLine()+","+problem.getColumn()+","+problem.getMessage());
            }
        }
        assertTrue(r.isValid());

        assertEquals(etalon,result);
    }

    @Test
    public void testTransformReject()throws Exception{
        StandardClientIncomeMessage standardClientIncomeMessage = EasyMock.createMock(StandardClientIncomeMessage.class);
        Element bodyElement = UsbXmlUtil.convertStringToDocument("<ns1:emptyData xmlns:ns1=\"urn://111\"></ns1:emptyData>").getDocumentElement();
        EasyMock.expect(standardClientIncomeMessage.getBody()).andReturn(bodyElement);
        EasyMock.expect(standardClientIncomeMessage.getAttachments()).andReturn(null).anyTimes();

        EasyMock.replay(standardClientIncomeMessage);
        SmevMessageData smevMessageData = transformation.transform(standardClientIncomeMessage);
        EasyMock.verify(standardClientIncomeMessage);

        Assert.assertNotNull(smevMessageData);
        SmevOutcomeRejectParams rejectParams = smevMessageData.getRejectParams();
        RejectCode rejectCode = rejectParams.getRejectCode();
        String rejectMessage = rejectParams.getRejectMessage();
        assertEquals(RejectCode.NO_DATA, rejectCode);
        assertEquals("NO_DATA", rejectMessage);
    }
}