package cz.devconf2021.lra.jpa;

/**
 * The logic starts with {@link #IN_PROGRESS} status
 * and on <code>complete</code>/<code>compensate</code>
 * the status is changed on {@link #SUCCESS}/{@link #FAILURE}.
 */
public enum TaskStatus {
    IN_PROGRESS, SUCCESS, FAILURE
}
