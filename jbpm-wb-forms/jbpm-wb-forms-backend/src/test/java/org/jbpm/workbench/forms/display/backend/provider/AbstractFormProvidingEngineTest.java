/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.workbench.forms.display.backend.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.enterprise.inject.Instance;

import org.jbpm.workbench.forms.display.api.KieWorkbenchFormRenderingSettings;
import org.jbpm.workbench.forms.service.providing.RenderingSettings;
import org.junit.Before;
import org.junit.Test;
import org.kie.internal.task.api.ContentMarshallerContext;
import org.kie.workbench.common.forms.dynamic.backend.server.context.generation.dynamic.impl.BackendFormRenderingContextManagerImpl;
import org.kie.workbench.common.forms.dynamic.backend.server.context.generation.dynamic.impl.FormValuesProcessorImpl;
import org.kie.workbench.common.forms.dynamic.backend.server.context.generation.dynamic.impl.fieldProcessors.MultipleSubFormFieldValueProcessor;
import org.kie.workbench.common.forms.dynamic.backend.server.context.generation.dynamic.impl.fieldProcessors.SubFormFieldValueProcessor;
import org.kie.workbench.common.forms.dynamic.backend.server.context.generation.dynamic.validation.impl.ContextModelConstraintsExtractorImpl;
import org.kie.workbench.common.forms.dynamic.service.context.generation.dynamic.BackendFormRenderingContext;
import org.kie.workbench.common.forms.dynamic.service.context.generation.dynamic.BackendFormRenderingContextManager;
import org.kie.workbench.common.forms.dynamic.service.context.generation.dynamic.FieldValueProcessor;
import org.kie.workbench.common.forms.dynamic.service.context.generation.dynamic.FormValuesProcessor;
import org.kie.workbench.common.forms.fields.test.TestFieldManager;
import org.kie.workbench.common.forms.fields.test.TestMetaDataEntryManager;
import org.kie.workbench.common.forms.jbpm.server.service.formGeneration.impl.runtime.BPMNRuntimeFormGeneratorService;
import org.kie.workbench.common.forms.jbpm.server.service.impl.DynamicBPMNFormGeneratorImpl;
import org.kie.workbench.common.forms.jbpm.service.bpmn.DynamicBPMNFormGenerator;
import org.kie.workbench.common.forms.serialization.FormDefinitionSerializer;
import org.kie.workbench.common.forms.serialization.impl.FieldSerializer;
import org.kie.workbench.common.forms.serialization.impl.FormDefinitionSerializerImpl;
import org.kie.workbench.common.forms.serialization.impl.FormModelSerializer;
import org.mockito.Mock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.*;

public abstract class AbstractFormProvidingEngineTest<SETTINGS extends RenderingSettings, PROCESSOR extends KieWorkbenchFormsValuesProcessor<SETTINGS>, PROVIDER extends AbstractKieWorkbenchFormsProvider> {

    protected static final String SERVER_TEMPLATE_ID = "serverTemplateId";

    @Mock
    protected ContentMarshallerContext marshallerContext;

    protected FormDefinitionSerializer formSerializer;

    protected BackendFormRenderingContextManagerImpl contextManager;

    protected FormValuesProcessor formValuesProcessor;

    protected DynamicBPMNFormGenerator dynamicBPMNFormGenerator;

    protected PROCESSOR processor;

    protected SETTINGS settings;

    protected PROVIDER workbenchFormsProvider;

    @Before
    public void initTest() {

        when(marshallerContext.getClassloader()).thenReturn(AbstractFormProvidingEngineTest.class.getClassLoader());

        formSerializer = new FormDefinitionSerializerImpl(new FieldSerializer(),
                                                          new FormModelSerializer(),
                                                          new TestMetaDataEntryManager());

        List<FieldValueProcessor> processors = Arrays.asList(new SubFormFieldValueProcessor(),
                                                             new MultipleSubFormFieldValueProcessor());

        Instance fieldValueProcessors = mock(Instance.class);
        when(fieldValueProcessors.iterator()).then(proc -> processors.iterator());

        formValuesProcessor = new FormValuesProcessorImpl(fieldValueProcessors);

        dynamicBPMNFormGenerator = new DynamicBPMNFormGeneratorImpl(new BPMNRuntimeFormGeneratorService(new TestFieldManager()));

        contextManager = new BackendFormRenderingContextManagerImpl(formValuesProcessor,
                                                                    new ContextModelConstraintsExtractorImpl());

        settings = generateSettings();

        processor = getProcessorInstance(formSerializer,
                                         contextManager,
                                         dynamicBPMNFormGenerator);

        initFormsProvider();
    }

    protected abstract void initFormsProvider();

    protected abstract SETTINGS generateSettings();

    protected abstract PROCESSOR getProcessorInstance(FormDefinitionSerializer formSerializer,
                                                      BackendFormRenderingContextManager contextManager,
                                                      DynamicBPMNFormGenerator dynamicBPMNFormGenerator);

    protected abstract Map<String, Object> getFormValues();

    protected abstract void checkRuntimeValues(Map<String, Object> resultValues);

    @Test
    public void testGenerateRenderingContext() {
        generateRenderingSettings();
    }

    protected KieWorkbenchFormRenderingSettings generateRenderingSettings() {
        KieWorkbenchFormRenderingSettings settings = processor.generateRenderingContext(this.settings);

        checkRenderingSettings(settings);

        return settings;
    }

    protected void checkRenderingSettings(KieWorkbenchFormRenderingSettings settings) {
        assertNotNull("Settings cannot be null",
                      settings);

        BackendFormRenderingContext context = contextManager.getContext(settings.getTimestamp());

        assertNotNull("There should be a backend context",
                      context);

        assertFalse(context.getAttributes().isEmpty());
        assertEquals(2,
                     context.getAttributes().size());
        assertNotNull(context.getAttributes().get(KieWorkbenchFormsValuesProcessor.SETTINGS_ATRA_NAME));
        assertNotNull(context.getAttributes().get(KieWorkbenchFormsValuesProcessor.SERVER_TEMPLATE_ID));

        assertFalse("There should exist some forms...",
                    settings.getRenderingContext().getAvailableForms().isEmpty());

        assertNotNull("A root form should exist",
                      settings.getRenderingContext().getRootForm());
    }

    @Test
    public void testGenerateRuntimeValuesMap() {
        KieWorkbenchFormRenderingSettings settings = generateRenderingSettings();

        Map<String, Object> result = processor.generateRuntimeValuesMap(settings.getTimestamp(),
                                                                        getFormValues());

        assertNotNull("Result values cannot be null",
                      result);

        assertFalse("Result cannot be empty",
                    result.isEmpty());

        checkRuntimeValues(result);
    }
}
