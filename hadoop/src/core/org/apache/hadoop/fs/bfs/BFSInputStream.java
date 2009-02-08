package org.apache.hadoop.fs.bfs;

import java.io.IOException;

import org.apache.hadoop.fs.FSInputStream;

import bfs.BFSClient;
import bfs.BFSFileInfo;

public class BFSInputStream extends FSInputStream {
	private BFSFileInfo fileInfo;
	private BFSClient bfs;
	private boolean isClosed;
	private long position;
	private long length;

	public BFSInputStream(BFSFileInfo fileInfo, BFSClient bfs) {
		if (fileInfo.isDirectory())
			throw new IllegalArgumentException("can't read from directory");

		this.fileInfo = fileInfo;
		this.bfs = bfs;
		this.isClosed = false;
	}

	@Override
	public long getPos() throws IOException {
		return this.position;
	}

	@Override
	public void seek(long targetPos) throws IOException {
		if (targetPos > this.length)
			throw new IOException("cannot seek past end of file");

		this.position = targetPos;
	}

	@Override
	public boolean seekToNewSource(long targetPos) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int read(byte[] buf, int offset, int len) throws IOException {
		// TODO Auto-generated method stub
		return -1;
	}

	/*
	 * We don't support marks, for now.
	 */
	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void mark(int readLimit) {
		// Do nothing
	}

	@Override
	public void reset() throws IOException {
		throw new IOException("Mark not supported");
	}
}
