package com.gvenzl.simplethreadpool;

import java.util.LinkedList;

/**
 * The Executor class is the driver class for the thread pool.
 * It controls size, start/stop of the thread pool.
 * @author gvenzl
 *
 */
public class Executor
{
	/**
	 * Holds the thread pool.
	 */
	private LinkedList<Thread> pool;

	/**
	 * Thread pool size.
	 */
	private int threadPoolSize = Integer.MAX_VALUE;
	/**
	 * Maximum thread pool size.
	 * This is a control mechanism to make sure the system cannot
	 * be overloaded by starting too many threads.
	 * For example, this can be set to 1000 and the simple thread pool
	 * won't allow you to start more than 1000 threads, regardless whether
	 * the {@code threadPoolSize} is greater.
	 */
	private int maxThreadPoolSize;
	
	/**
	 * Indicates whether the thread pool has been terminated, i.e. stopped
	 */
	private boolean terminated = false;
	
	/**
	 * Indicates whether the thread pool is currently running.
	 */
	private boolean running = false;
	
	/**
	 * The submission is the Runnable to be executed.
	 */
	private Class<? extends Runnable> submission;
	
	/**
	 * Returns an {@code Executor} instance
	 * @param task The Runnable to execute within the thread pool.
	 */
	public Executor(Class<? extends Runnable> task) {
		pool = new LinkedList<Thread>();
		submission = task;
	}
	
	/**
	 * Returns an {@code Executor} instance
	 * @param poolSize The thread pool size
	 * @param task The Runnable to execute within the thread pool.
	 */
	public Executor(int poolSize, Class<? extends Runnable> task) {
		threadPoolSize = poolSize;
		pool = new LinkedList<Thread>();
		submission = task;
	}
	
	/**
	 * Returns an {@code Executor} instance
	 * @param poolSize The thread pool size
	 * @param maxPoolSize The maximum thread pool size
	 * @param task The Runnable to execute within the thread pool.
	 */
	public Executor(int poolSize, int maxPoolSize, Class<? extends Runnable> task) {
		threadPoolSize = poolSize;
		maxThreadPoolSize = maxPoolSize;
		pool = new LinkedList<Thread>();
		submission = task;
	}
	
	/**
	 * Sets the thread pool size
	 * @param size The size of the thread pool
	 * @throws IllegalArgumentException An {@code IllegalArgumentExcpetion} is thrown
	 * if the pool size is greater than the max pool size.
	 */
	public void setPoolSize(int size) throws IllegalArgumentException {
		if (size > maxThreadPoolSize) {
			throw new IllegalArgumentException("Thread pool size greater than max thread pool size");
		}
	
		// Only perform actual resizing if the pool is currently running.
		if(running) {
			if (size > threadPoolSize) {
				for (int i=threadPoolSize; i<size; i++) {
					Thread t;
					try{
						t = new Thread(submission.newInstance());
						t.start();
						pool.add(t);
					}
					catch (InstantiationException | IllegalAccessException e) {
						shutdown();
						e.printStackTrace();
					}
				}
			}
			else if (size < threadPoolSize) {
				LinkedList<Thread> kill = new LinkedList<Thread>();
				// Interrupt all the threads at once until the threadPoolSize is equal to the new size
				for (int i=threadPoolSize; i>size; i--) {
					Thread t = pool.remove();
					t.interrupt();
					kill.add(t);
				}
				
				// Kill the threads and wait for them to join
				for (Thread t : kill) {
					try {
						t.join();
					}
					catch (InterruptedException e) {
						// Interrupted, break loop
						break;
					}
				}
			}
		}
		// After resizing is done (or not if the pool isn't running) set the threadPoolSize accordingly
		threadPoolSize = size;
	}
	
	/**
	 * Returns the thread pool size
	 * @return The thread pool size
	 */
	public int getPoolSize() {
		return threadPoolSize;
	}
	
	/**
	 * Sets the max thread pool size.
	 * Regardless of what the thread pool size is set to the pool will never
	 * exceed the max thread pool size.
	 * @param maxSize The maximum number of threads within the pool
	 */
	public void setMaxPoolSize(int maxSize) {
		maxThreadPoolSize = maxSize;
	}
	
	/**
	 * Returns the maximum thread pool size.
	 * @return The maximum thread pool size.
	 */
	public int getMaxPoolSize() {
		return maxThreadPoolSize;
	}
	

	/**
	 * Submits a task to the thread pool.
	 * All the threads will work on a separate copy of the task.
	 * Thread safety is not guaranteed.
	 * @param task The task to work on.
	 */
	public void submit(Class<? extends Runnable> task) {
		submission = task;
	}

	/**
	 * Runs the tasks within the pool
	 * @throws IllegalStateException One of the following: <br/> <li>submission is null</li>
	 * @throws IllegalAccessException 
	 * @throws InstantiationException The Executor couldn't create a new instance for the submission.
	 * This happens normally when there is no default constructor.
	 */
	public void run() throws IllegalStateException, InstantiationException, IllegalAccessException {
		if (!running) {
			if (null == submission) {
				throw new IllegalStateException("No submission passed");
			}
	
			for (int i=pool.size(); i< threadPoolSize &&
					      i<= maxThreadPoolSize; i++) {
				Thread t;
				try {
					t = new Thread(submission.newInstance());
					t.start();
					pool.add(t);
				}
				catch(InstantiationException | IllegalAccessException e) {
					shutdown();
					throw e;
				}
			}
			
			running = true;
			terminated = false;
		}
	}
	
	/**
	 * Shuts the thread pool down in a graceful manner.
	 * The task submitted have to respond to the thread interrupt.
	 */
	public void shutdown() {
		while(!pool.isEmpty()) {
			Thread t = pool.remove();
			t.interrupt();
		}
		terminated = true;
		running = false;
	}
	
	/**
	 * Indicates whether a thread pool is terminated or not.
	 * @return <b>True</b> if the thread pool is terminated (pool had to be started first). <br/>
	 * <b>False</b> if the pool has not been terminated or started.
	 */
	public boolean isTerminated() {
		return terminated;
	}
	
	/**
	 * Indicates whether the thread pool is currently running or not
	 * @return <b>True</b> if the thread pool is currently running. <br/>
	 * <b>False</b> if the thread pool has not been started yet or terminated.
	 */
	public boolean isRunning() {
		return running;
	}
}
