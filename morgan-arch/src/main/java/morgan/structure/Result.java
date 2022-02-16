package morgan.structure;

import morgan.support.Log;

import java.util.HashMap;
import java.util.Map;

public class Result {

    private Map<String, Object> contexts = new HashMap<>();
    private Map<String, Object> results = new HashMap<>();

    private boolean isError = false;

    public void setContexts(Object...contexts){
        try{
            if (contexts == null)
                return;
            if (contexts.length == 1){
                this.contexts.put("context", contexts[0]);
                return;
            }
            for (int i = 0; i < (contexts.length - 1); i += 2){
                this.contexts.put((String)contexts[i], contexts[i + 1]);
            }
        } catch (ClassCastException e){
            Log.worker.error("contexts must be string-object pair");
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e){
            Log.worker.error("contexts must be even pairs");
            e.printStackTrace();
        }
    }

    public void setResults(Object...results){
        try{
            if (results == null)
                return;
            if (results.length == 1){
                this.results.put("result", results[0]);
                return;
            }
            for (int i = 0; i < (results.length - 1); i += 2){
                this.results.put((String)results[i], results[i + 1]);
            }
        } catch (ClassCastException e){
            Log.worker.error("results must be string-object pair");
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e){
            Log.worker.error("results must be even pairs");
            e.printStackTrace();
        }
    }

    public void setError(boolean isError){
        this.isError = isError;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult(String key){
        return (T) results.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext(String key){
        return (T) contexts.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult(){
        return (T) results.get("result");
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext(){
        return (T) contexts.get("context");
    }

    public boolean isError(){
        return isError;
    }
}
