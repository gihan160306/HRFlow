import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Gọi API khi mount (và khi `deps` đổi). Trả về { data, error, loading, reload }.
 * Bỏ qua kết quả của request cũ khi component unmount hoặc deps đổi.
 */
export function useAsync(fn, deps = [], { enabled = true } = {}) {
  const [state, setState] = useState({ data: null, error: null, loading: enabled });
  const fnRef = useRef(fn);
  fnRef.current = fn;
  const seq = useRef(0);

  const run = useCallback(async () => {
    const id = ++seq.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const data = await fnRef.current();
      if (id === seq.current) setState({ data, error: null, loading: false });
    } catch (err) {
      if (id === seq.current) setState({ data: null, error: err, loading: false });
    }
  }, []);

  useEffect(() => {
    if (!enabled) return undefined;
    run();
    return () => {
      seq.current += 1;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, run, ...deps]);

  return { ...state, reload: run };
}

export default useAsync;
