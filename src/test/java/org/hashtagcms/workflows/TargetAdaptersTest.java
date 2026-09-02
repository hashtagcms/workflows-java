package org.hashtagcms.workflows;

import org.hashtagcms.workflows.engine.WorkflowContext;
import org.hashtagcms.workflows.engine.WorkflowHandler;
import org.hashtagcms.workflows.engine.WorkflowResponse;
import org.hashtagcms.workflows.engine.target.CustomClassTargetAdapter;
import org.hashtagcms.workflows.engine.target.EventTargetAdapter;
import org.hashtagcms.workflows.engine.target.ServiceTargetAdapter;
import org.hashtagcms.workflows.engine.target.TargetResult;
import org.hashtagcms.workflows.model.Workflow;
import org.hashtagcms.workflows.service.WorkflowHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Unit tests for the service / event / custom_class target adapters. */
class TargetAdaptersTest {

    // ---- service ------------------------------------------------------------

    public static class Greeter {
        public Map<String, Object> handle(Map<String, Object> args) {
            return Map.of("greeting", "hi " + args.getOrDefault("name", "there"));
        }
    }

    @Test
    void serviceAdapterInvokesBeanMethodAndReturnsBody() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean("greeter")).thenReturn(new Greeter());
        ServiceTargetAdapter adapter = new ServiceTargetAdapter(ctx);

        TargetResult r = adapter.execute(
                Map.of("class", "greeter", "method", "handle", "arguments", Map.of("name", "Sam")),
                Map.of());

        assertThat(r.success()).isTrue();
        assertThat(r.body()).isEqualTo(Map.of("greeting", "hi Sam"));
    }

    @Test
    void serviceAdapterReadsNestedServiceKeyAndErrorsOnUnknownBean() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean("missing")).thenThrow(new NoSuchBeanDefinitionException("missing"));
        ServiceTargetAdapter adapter = new ServiceTargetAdapter(ctx);

        TargetResult r = adapter.execute(Map.of("service", Map.of("class", "missing")), Map.of());

        assertThat(r.success()).isFalse();
        assertThat(r.status()).isEqualTo(500);
        assertThat(r.error()).contains("Service call failed");
    }

    // ---- event --------------------------------------------------------------

    @Test
    void eventAdapterPublishesDomainEvent() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        EventTargetAdapter adapter = new EventTargetAdapter(publisher);

        TargetResult r = adapter.execute(
                Map.of("class", "order.placed", "payload", Map.of("orderId", 99)),
                Map.of());

        ArgumentCaptor<EventTargetAdapter.WorkflowDomainEvent> captor =
                ArgumentCaptor.forClass(EventTargetAdapter.WorkflowDomainEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("order.placed");
        assertThat(captor.getValue().payload()).isEqualTo(Map.of("orderId", 99));

        assertThat(r.success()).isTrue();
        assertThat(((Map<?, ?>) r.body()).get("dispatched")).isEqualTo(true);
    }

    // ---- custom_class / handler --------------------------------------------

    private WorkflowContext context() {
        Workflow wf = new Workflow();
        wf.setAlias("WF_TEST");
        return new WorkflowContext(wf, Map.of(), 1, "web", "1.0.0", List.of(), Map.of());
    }

    @Test
    void customClassAdapterRunsRegisteredHandlerAndCarriesResponse() {
        WorkflowHandlerRegistry registry = new WorkflowHandlerRegistry();
        registry.register("WF_TEST", c -> WorkflowResponse.make().setMessage("done").toast("t", "success"));
        CustomClassTargetAdapter adapter =
                new CustomClassTargetAdapter(mock(ApplicationContext.class), registry);

        TargetResult r = adapter.execute(
                Map.of("class", "WF_TEST"),
                Map.of("workflow_context", context()));

        assertThat(r.success()).isTrue();
        assertThat(r.workflowResponse()).isNotNull();
        assertThat(r.workflowResponse().getMessage()).isEqualTo("done");
    }

    @Test
    void customClassAdapterErrorsWhenHandlerUnresolved() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean("nope")).thenThrow(new NoSuchBeanDefinitionException("nope"));
        CustomClassTargetAdapter adapter = new CustomClassTargetAdapter(ctx, new WorkflowHandlerRegistry());

        TargetResult r = adapter.execute(Map.of("class", "nope"), Map.of("workflow_context", context()));

        assertThat(r.success()).isFalse();
        assertThat(r.error()).contains("could not be resolved");
    }

    @Test
    void customClassAdapterErrorsWithoutWorkflowContext() {
        WorkflowHandlerRegistry registry = new WorkflowHandlerRegistry();
        registry.register("WF_TEST", c -> WorkflowResponse.make());
        CustomClassTargetAdapter adapter =
                new CustomClassTargetAdapter(mock(ApplicationContext.class), registry);

        TargetResult r = adapter.execute(Map.of("class", "WF_TEST"), Map.of()); // no workflow_context

        assertThat(r.success()).isFalse();
        assertThat(r.error()).contains("workflow context");
    }
}
