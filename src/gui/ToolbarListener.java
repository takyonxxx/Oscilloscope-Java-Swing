package gui;

public interface ToolbarListener {
	public void startEventOccured();
	public void stopEventOccured();
	public void sliderChangedEventOccured(int value);
	public void exitEventOccured();
}
