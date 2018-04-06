package de.srlabs.patchalyzer;

/**
 * This class can hold results or exception we get, when performing a single basic test
 */
public class BasicTestResult {

    private String basicTestUUID;
    private Boolean result;
    private String exception;

    public BasicTestResult(String uuid, Boolean result, String exception){
        this.basicTestUUID = uuid;
        this.result = result;
        this.exception = exception;
    }

    public String getException(){
        return exception;
    }

    public String getBasicTestUUID(){
        return basicTestUUID;
    }

    public Boolean getResult(){
        return result;
    }

    @Override
    public String toString(){
        if(exception != null){
            return "basicTest: "+basicTestUUID+" with exception:"+exception;
        }
        else
            return "basicTest:"+basicTestUUID+" with result:"+result;
    }
}
