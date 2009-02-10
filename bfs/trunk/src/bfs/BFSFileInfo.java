package bfs;

public class BFSFileInfo {
	private boolean isDirectory;
	private String fileName;

	public BFSFileInfo(String name) {
		this.fileName = name;
		this.isDirectory = false;
	}

	public boolean isDirectory() {
		return this.isDirectory;
	}

	public String getPath() {
		return "/" + this.fileName;
	}

	public String getName() {
		return this.fileName;
	}

	public int getReplication() {
		return Conf.getRepFactor();
	}

	public long getChunkSize() {
		return Conf.getChunkSize();
	}

	// TODO
	public long getLength() {
		return 0;
	}
}
