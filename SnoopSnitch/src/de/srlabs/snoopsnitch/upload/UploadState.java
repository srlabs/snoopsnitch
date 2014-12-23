package de.srlabs.snoopsnitch.upload;

import java.io.Serializable;
import java.util.Vector;

public class UploadState implements Serializable{
	private static final long serialVersionUID = 1L;
	public enum State {
		IDLE, // Initial state, set in UploadServiceHelper.createUploadState()
		RUNNING, // Upload is running
		COMPLETED, // All files have been successfully uploaded
		FAILED, // Uploading was cancelled due to an error 
		STOPPED // Uploading was stopped with MSG_UPLOAD_STOP
	}
	private State state;
	private DumpFile allFiles[];
	private Vector<DumpFile> completedFiles;
	private long totalSize;
	private long uploadedSize;
	private String errorStr;
	/**
	 * Changes the state to State.FAILED and set an error string
	 * @param errorStr
	 */
	public void error(String errorStr) {
		this.state = State.FAILED;
		this.errorStr = errorStr;
	}
	public UploadState(State state, DumpFile[] allFiles,
			long totalSize, long uploadedSize, String errorStr) {
		this.state = state;
		this.allFiles = allFiles;
		this.completedFiles = new Vector<DumpFile>();
		this.totalSize = totalSize;
		this.uploadedSize = uploadedSize;
		this.errorStr = errorStr;
	}
	public DumpFile[] getAllFiles() {
		return allFiles;
	}
	public String[] getCompletedFiles() {
		return completedFiles.toArray(new String[0]);
	}
	public long getTotalSize() {
		return totalSize;
	}
	public long getUploadedSize() {
		return uploadedSize;
	}
	public State getState() {
		return state;
	}
	public String getErrorStr() {
		return errorStr;
	}
	public void addCompletedFile(DumpFile file, long size){
		completedFiles.add(file);
		uploadedSize += size;
	}
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("UploadState: " + state.name() + "\n");
		if(state == State.FAILED)
			result.append("ERROR: " + errorStr + "\n");
		result.append("Total size: " + totalSize / 1024 + " KiB   Done: " + uploadedSize/1024 + " KiB\n");
		result.append("Uploaded:\n");
		if(completedFiles.isEmpty())
			result.append("None\n");
		else{
			for(DumpFile file: completedFiles)
				result.append(file.getFilename() + "\n");
		}
		result.append("\nPending files:\n");
		boolean pendingFilesExist = false;
		for(DumpFile file:allFiles){
			boolean fileIsCompleted = false;
			for(DumpFile tmp:completedFiles)
				if(tmp.getId() == file.getId())
					fileIsCompleted = true;
			if(!fileIsCompleted){
				result.append(file.getFilename() + "\n");
				pendingFilesExist = true;
			}
		}
		if(!pendingFilesExist){
			result.append("None");
		}
		result.append("\n");
		return result.toString();
	}
	public void setState(State state) {
		this.state = state;		
	}
}
