package com.ui.factory;

import java.util.function.Consumer;
import java.util.function.Function;

public final class ServiceResult<T> {
    private final ResultType type;
    private final T data;
    private final String message;
    private final Exception exception;

    private ServiceResult(ResultType type, T data, String message, Exception exception) {
        this.type = type; this.data = data; this.message = message; this.exception = exception;
    }
    public static <T> ServiceResult<T> success(T data) { return new ServiceResult<>(ResultType.SUCCESS, data, "OK", null); }
    public static <T> ServiceResult<T> error(String msg, Exception ex) { return new ServiceResult<>(ResultType.ERROR, null, msg, ex); }
    
    public boolean isSuccess() { return type == ResultType.SUCCESS; }
    public boolean isError() { return !isSuccess(); }
    public T getData() { if(isError()) throw new IllegalStateException("No hay datos en un resultado fallido"); return data; }
    public String getMessage() { return message; }
    
    public <U> ServiceResult<U> flatMap(Function<T, ServiceResult<U>> mapper) {
        if (isSuccess()) {
            try { return mapper.apply(data); } catch (Exception e) { return error("Error en flatMap", e); }
        }
        return new ServiceResult<>(this.type, null, this.message, this.exception);
    }
    
    public ServiceResult<T> ifSuccess(Consumer<T> action) { if (isSuccess()) action.accept(data); return this; }
    public ServiceResult<T> ifError(Consumer<String> action) { if (isError()) action.accept(message); return this; }

    public String getErrorMessage() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    public enum ResultType { SUCCESS, ERROR }
}
