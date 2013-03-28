package org.motechproject.mobileforms.api.web;

import com.jcraft.jzlib.ZInputStream;
import org.fcitmuk.epihandy.EpihandyXformSerializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.motechproject.mobileforms.api.domain.Form;
import org.motechproject.mobileforms.api.domain.FormGroup;
import org.motechproject.mobileforms.api.service.MobileFormsService;
import org.motechproject.mobileforms.api.service.UsersService;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FormDownloadServletTest {
    private FormDownloadServlet formDownloadServlet;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private MobileFormsService mobileFormsService;
    @Mock
    private UsersService usersService;
    @Mock
    private EpihandyXformSerializer epihandySerializer;
    @Mock
    private Properties mobileFormsProperties;

    @Before
    public void setUp() {
        initMocks(this);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        when(applicationContext.getBean("mobileFormsProperties", Properties.class)).thenReturn(mobileFormsProperties);
        formDownloadServlet = spy(new FormDownloadServlet(applicationContext));
        ReflectionTestUtils.setField(formDownloadServlet, "mobileFormsService", mobileFormsService);
        ReflectionTestUtils.setField(formDownloadServlet, "usersService", usersService);
        doReturn(epihandySerializer).when(formDownloadServlet).serializer();
    }

    @Test
    public void shouldReturnListOfStudyNames() {
        String studyOne = "StudyOne";
        String studyTwo = "StudyTwo";
        final String dataExpectedToBeReturned = "Mock - List of form groups";
        when(mobileFormsService.getAllFormGroups()).thenReturn(asList(new Object[]{0, studyOne}, new Object[]{1, studyTwo}));
        try {
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    ((ByteArrayOutputStream) invocationOnMock.getArguments()[0]).write(dataExpectedToBeReturned.getBytes());
                    return null;
                }
            }).when(epihandySerializer).serializeStudies(any(OutputStream.class), Matchers.anyObject());
            populateHttpRequest(request, "username", "password", FormDownloadServlet.ACTION_DOWNLOAD_STUDY_LIST, null);

            formDownloadServlet.doPost(request, response);
            String responseSentToMobile = readResponse(response);

            ArgumentCaptor<OutputStream> outputStreamArgumentCaptor = ArgumentCaptor.forClass(OutputStream.class);
            ArgumentCaptor<Object> dataArgumentCaptor = ArgumentCaptor.forClass(Object.class);

            verify(epihandySerializer).serializeStudies(outputStreamArgumentCaptor.capture(), dataArgumentCaptor.capture());
            assertThat(outputStreamArgumentCaptor.getValue().toString(), is(equalTo(dataExpectedToBeReturned)));
            assertThat(((List<Object[]>) dataArgumentCaptor.getValue()).size(), is(equalTo(2)));
            assertTrue(Arrays.equals(((List<Object[]>) dataArgumentCaptor.getValue()).get(0), new Object[]{0, studyOne}));
            assertTrue(Arrays.equals(((List<Object[]>) dataArgumentCaptor.getValue()).get(1), new Object[]{1, studyTwo}));
            assertThat(responseSentToMobile, is(equalTo((char) FormDownloadServlet.RESPONSE_SUCCESS + dataExpectedToBeReturned)));
        } catch (Exception e) {
            assertFalse("Test failed, cause: " + e.getMessage(), true);
        }
    }

    @Test
    public void shouldReturnListOfFormsForTheGivenStudyNameWithListOfUserAccounts() {
        final String formOneContent = "FormOne";
        final String formTwoContent = "FromTwo";
        final String groupName = "GroupOne";
        final Integer groupIndex = 2;
        final String dataExpectedToBeReturned = "Mock - List of forms";
        final String userDetailsExpectedToBeReturned = "Mock - User details";

        List<Object[]> userDetails = new ArrayList<Object[]>();
        userDetails.add(new Object[]{"username", "password", "salt"});
        when(usersService.getUsers()).thenReturn(userDetails);

        final List<String> formXmlContents = Arrays.asList(formOneContent, formTwoContent);
        final List<Form> forms = Arrays.asList(new Form(null, null, formOneContent, null, null, null, null),
                new Form(null, null, formTwoContent, null, null, null, null));
        when(mobileFormsService.getForms(groupIndex)).thenReturn(new FormGroup(groupName, forms));

        try {
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    ((ByteArrayOutputStream) invocationOnMock.getArguments()[0]).write(userDetailsExpectedToBeReturned.getBytes());
                    List<Object[]> userDetails = (List<Object[]>) invocationOnMock.getArguments()[1];
                    assertThat(userDetails.size(), is(equalTo(1)));
                    assertThat(userDetails.get(0)[0], is(equalTo(userDetails.get(0)[0])));
                    assertThat(userDetails.get(0)[1], is(equalTo(userDetails.get(0)[1])));
                    assertThat(userDetails.get(0)[2], is(equalTo(userDetails.get(0)[2])));
                    return null;
                }
            }).when(epihandySerializer).serializeUsers(any(OutputStream.class), Matchers.anyObject());

            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    ((ByteArrayOutputStream) invocationOnMock.getArguments()[0]).write(dataExpectedToBeReturned.getBytes());
                    assertThat((List<String>) invocationOnMock.getArguments()[1], is(equalTo(formXmlContents)));
                    assertThat((Integer) invocationOnMock.getArguments()[2], is(equalTo(groupIndex)));
                    assertThat((String) invocationOnMock.getArguments()[3], is(equalTo(groupName)));
                    return null;
                }
            }).when(epihandySerializer).serializeForms(any(OutputStream.class), Matchers.anyObject(), anyInt(), anyString());

            populateHttpRequest(request, "username", "password", FormDownloadServlet.ACTION_DOWNLOAD_USERS_AND_FORMS, groupIndex);

            formDownloadServlet.doPost(request, response);
            String responseSentToMobile = readResponse(response);

            assertThat(responseSentToMobile, is(equalTo((char) FormDownloadServlet.RESPONSE_SUCCESS + userDetailsExpectedToBeReturned + dataExpectedToBeReturned)));
        } catch (Exception e) {
            assertFalse("Test failed, cause: " + e.getMessage(), true);
        }
    }

    private String readResponse(MockHttpServletResponse response) throws IOException {
        return new BufferedReader(new InputStreamReader(new ZInputStream(new ByteArrayInputStream(response.getContentAsByteArray())))).readLine();
    }

    private void populateHttpRequest(MockHttpServletRequest request, String userName,
                                     String password, byte actionCode,
                                     Integer groupIndex) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeUTF(userName);
        dataOutputStream.writeUTF(password);
        dataOutputStream.writeUTF("epihandyser");
        dataOutputStream.writeUTF("en");
        dataOutputStream.writeByte(actionCode);

        if (groupIndex != null) {
            dataOutputStream.writeInt(groupIndex);
        }
        request.setContent(byteArrayOutputStream.toByteArray());
    }
}
