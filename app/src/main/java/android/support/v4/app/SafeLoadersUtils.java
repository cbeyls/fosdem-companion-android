package android.support.v4.app;

/**
 * Contains utility method to prevent Loaders from being forcefully retained during a configuration change.
 * Forceful retain currently causes all stopped Loaders to briefly start, causing unexpected issues for detached fragments.
 * This restores the Loaders behavior of support libraries < 24.0.0 for fragments.
 *
 * @author Christophe Beyls
 * @see <a href="https://issuetracker.google.com/issues/37916599">Bug report</a>
 */
public class SafeLoadersUtils {

	public static void onRetainCustomNonConfigurationInstance(FragmentActivity activity) {
		// All loaders are already stopped or retained at that point, but calling this method again
		// sets a flag to prevent them from being forcefully retained during the next phase
		activity.mFragments.doLoaderStop(false);
	}
}