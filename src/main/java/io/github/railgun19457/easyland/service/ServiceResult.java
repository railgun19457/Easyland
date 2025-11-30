package io.github.railgun19457.easyland.service;

/**
 * 服务层操作结果封装类
 * 用于统一处理服务层的返回结果，包含成功/失败状态、数据和错误信息
 *
 * @param <T> 返回数据的类型
 */
public class ServiceResult<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private ServiceResult(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    /**
     * 创建成功结果
     *
     * @param data 返回的数据
     * @param <T>  数据类型
     * @return 成功的 ServiceResult
     */
    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, data, null);
    }

    /**
     * 创建成功结果（无数据）
     *
     * @param <T> 数据类型
     * @return 成功的 ServiceResult
     */
    public static <T> ServiceResult<T> success() {
        return new ServiceResult<>(true, null, null);
    }

    /**
     * 创建失败结果
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败的 ServiceResult
     */
    public static <T> ServiceResult<T> failure(String message) {
        return new ServiceResult<>(false, null, message);
    }

    /**
     * 判断操作是否成功
     *
     * @return true 表示成功，false 表示失败
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 判断操作是否失败
     *
     * @return true 表示失败，false 表示成功
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * 获取返回的数据
     *
     * @return 数据对象，可能为 null
     */
    public T getData() {
        return data;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息，成功时为 null
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ServiceResult{" +
                "success=" + success +
                ", data=" + data +
                ", message='" + message + '\'' +
                '}';
    }
}
