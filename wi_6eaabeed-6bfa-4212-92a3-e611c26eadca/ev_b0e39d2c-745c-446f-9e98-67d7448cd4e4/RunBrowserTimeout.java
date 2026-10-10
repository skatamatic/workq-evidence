import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.engine.discovery.DiscoverySelectors;
public class RunBrowserTimeout {
 public static void main(String[] args) {
  var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectMethod("com.flatirons.insight.scenario.InsightBrowserLauncherTest", "shortestBoundedRunPublishesReadinessThenCleansUpOnTimeout")).build();
  var listener=new SummaryGeneratingListener(); var launcher=LauncherFactory.create();
  launcher.registerTestExecutionListeners(listener); launcher.execute(request);
  var summary=listener.getSummary(); var writer=new java.io.PrintWriter(System.out,true);
  summary.printTo(writer); summary.printFailuresTo(writer);
  System.exit(summary.getTestsSucceededCount()==1 && summary.getTestsFailedCount()==0 ? 0 : 1);
 }
}
