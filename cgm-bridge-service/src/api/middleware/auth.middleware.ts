import { Request, Response, NextFunction } from "express";

interface ServicePrincipal {
  tokenId: string;
}

declare global {
  namespace Express {
    interface Request {
      servicePrincipal?: ServicePrincipal;
    }
  }
}

export const requireServiceAuth = (tokens: string[]) => {
  const allowed = new Set(tokens);

  return (req: Request, res: Response, next: NextFunction): void => {
    if (allowed.size === 0) {
      next();
      return;
    }

    const auth = req.header("authorization")?.trim() ?? "";
    if (!auth.startsWith("Bearer ")) {
      res.status(401).json({
        type: "about:blank",
        title: "No autorizado",
        status: 401,
        code: "MISSING_BEARER_TOKEN",
        detail: "Se requiere Authorization: Bearer <token>.",
        traceId: req.header("x-request-id") ?? "no-trace-id"
      });
      return;
    }

    const token = auth.slice("Bearer ".length).trim();
    if (!allowed.has(token)) {
      res.status(403).json({
        type: "about:blank",
        title: "Prohibido",
        status: 403,
        code: "INVALID_SERVICE_TOKEN",
        detail: "Token de servicio inválido.",
        traceId: req.header("x-request-id") ?? "no-trace-id"
      });
      return;
    }

    req.servicePrincipal = { tokenId: token.slice(0, 6) };
    next();
  };
};
