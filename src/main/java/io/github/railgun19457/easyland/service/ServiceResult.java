package io.github.railgun19457.easyland.service;

/**
 * 服务层操作结果封装类 - 重构为Java 21 record
 * 用于统一处理服务层的返回结果，包含成功/失败状态、数据和错误信息
 *
 * @param <T> 返回数据的类型
 */
public record ServiceResult<T>(boolean isSuccess, T data, String message) {
    
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
     * 创建失败结果（带数据）
     *
     * @param data    返回的数据
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败的 ServiceResult
     */
    public static <T> ServiceResult<T> failure(T data, String message) {
        return new ServiceResult<>(false, data, message);
    }

    /**
     * 判断操作是否失败
     *
     * @return true 表示失败，false 表示成功
     */
    public boolean isFailure() {
        return !isSuccess;
    }

    /**
     * 使用Java 21模式匹配进行条件操作
     *
     * @param onSuccess 成功时执行的操作
     * @param onFailure 失败时执行的操作
     */
    public void match(java.util.function.Consumer<T> onSuccess, java.util.function.Consumer<String> onFailure) {
        if (isSuccess) {
            onSuccess.accept(data);
        } else {
            onFailure.accept(message);
        }
    }

    /**
     * 使用Java 21模式匹配进行转换操作
     *
     * @param onSuccess 成功时执行的转换函数
     * @param onFailure 失败时执行的转换函数
     * @param <R>       返回类型
     * @return 转换后的结果
     */
    public <R> R match(java.util.function.Function<T, R> onSuccess, java.util.function.Function<String, R> onFailure) {
        return isSuccess ? onSuccess.apply(data) : onFailure.apply(message);
    }

    /**
     * 链式操作，仅在成功时执行
     *
     * @param mapper 转换函数
     * @param <R>    新的数据类型
     * @return 新的ServiceResult
     */
    public <R> ServiceResult<R> map(java.util.function.Function<T, R> mapper) {
        return isSuccess ? success(mapper.apply(data)) : failure(message);
    }

    /**
     * 链式操作，仅在失败时执行
     *
     * @param mapper 错误信息转换函数
     * @return 新的ServiceResult
     */
    public ServiceResult<T> mapError(java.util.function.Function<String, String> mapper) {
        return isSuccess ? this : failure(mapper.apply(message));
    }

    /**
     * 链式操作，允许失败时转换为成功
     *
     * @param mapper 转换函数
     * @return 新的ServiceResult
     */
    public ServiceResult<T> recover(java.util.function.Function<String, ServiceResult<T>> mapper) {
        return isSuccess ? this : mapper.apply(message);
    }

    @Override
    public String toString() {
        return "ServiceResult{" +
                "success=" + isSuccess +
                ", data=" + data +
                ", message='" + message + '\'' +
                '}';
    }
}
