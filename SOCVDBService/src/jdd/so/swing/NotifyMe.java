package jdd.so.swing;

import java.io.File;

/**
 * Interface to pass to CloseVoteFinder if you like to be notfied
 * @author Petter Friberg
 *
 */
public interface NotifyMe {

	public void message(String text);
	public void done();
	public void done(File exportFile);
	public void done(String remoteUrl);
}
